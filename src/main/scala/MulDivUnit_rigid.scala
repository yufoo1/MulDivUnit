import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.util._
import chisel3.util.experimental.forceName

class MulDivUnitIO_rigid extends Bundle {
    val in = Input(new MulDivUnitIn())
    val out = Output(new MulDivUnitOut())
    val busy = Output(Bool())
}

class MulUnit_rigid extends Module {
    val io = IO(new MulDivUnitIO_rigid())
    val tmp = RegInit(0.U(64.W))
    val rigidCnt = RegInit(0.U(3.W))
    when(io.in.op === MulDivUnitOp.MUL & !io.busy) {
        tmp := Mux(io.in.sign, io.in.src.map(i => i.asSInt).reduce(_ * _).asUInt, io.in.src.reduce(_ * _))
        rigidCnt := 5.U
    }.elsewhen(rigidCnt > 0.U) {
        rigidCnt := rigidCnt - 1.U
    }
    io.out.res.zipWithIndex.foreach(i => i._1 := tmp(((i._2 + 1) << 5) - 1, i._2 << 5))
    io.busy := rigidCnt =/= 0.U
}

class DivUnit_rigid extends Module {
    val io = IO(new MulDivUnitIO_rigid())
    val negSrcBits = io.in.src.map(i => i(31) & io.in.sign)
    val negResBits = Reg(Vec(2, Bool()))
    val absSrc = io.in.src.zip(negSrcBits).map(i => Mux(i._2, -i._1, i._1))
    val absSrc64 = absSrc.zipWithIndex.map(i => Cat(0.U(32.W), i._1) << (i._2 << 5))
    val timer = RegInit(0.U(32.W))
    val tmps = Reg(Vec(4, UInt(67.W)))
    val subs = tmps.slice(1, 4).map(i => (tmps(0) << 2).asUInt - i)
    val tmp = Wire(Vec(2, UInt(32.W)))
    val busy = RegInit(false.B)
    val rigidCnt = RegInit(0.U(4.W))
    tmp.zipWithIndex.foreach(i => i._1 := tmps(0)(((i._2 + 1) << 5) - 1, i._2 << 5))
    io.out.res.zip(tmp.zip(negResBits)).foreach(i => i._1 := Mux(i._2._2, -i._2._1, i._2._1))
    io.busy := rigidCnt =/= 0.U
    when(io.in.op === MulDivUnitOp.DIV & !io.busy) {
        timer := "hFFFFFFFF".U
        negResBits(0) := negSrcBits.reduce(_ ^ _)
        negResBits(1) := negSrcBits(0)
        tmps(0) := absSrc64(0)
        tmps(1) := absSrc64(1)
        tmps(2) := Cat(0.U(1.W), absSrc64(1)) << 1
        tmps(3) := (Cat(0.U(1.W), absSrc64(1)) << 1).asUInt + absSrc64(1)
        busy := true.B
        rigidCnt := 15.U
    }.otherwise {
        when(rigidCnt > 0.U) {
            rigidCnt := rigidCnt - 1.U
        }
        when(timer(15) & (tmps(0)(47, 16) < tmps(1)(63, 32))) {
            timer := timer >> 16
            tmps(0) := tmps(0) << 16
        }.elsewhen(timer(7) & (tmps(0)(55, 24) < tmps(1)(63, 32))) {
            timer := timer >> 8
            tmps(0) := tmps(0) << 8
        }.elsewhen(timer(3) & (tmps(0)(59, 28) < tmps(1)(63, 32))) {
            timer := timer >> 4
            tmps(0) := tmps(0) << 4
        }.elsewhen(timer(0)) {
            timer := timer >> 2
            tmps(0) := Mux(!subs(2)(66), subs(2) + 3.U, Mux(!subs(1)(66), subs(1) + 2.U, Mux(!subs(0)(66), subs(0) + 1.U, tmps(0) << 2)))
        }
    }
}

class MulDivUnit_rigid extends Module {
    val io = IO(new MulDivUnitIO_rigid())
    val mulUnit = Module(new MulUnit_rigid)
    val divUnit = Module(new DivUnit_rigid)
    val op = RegNext(io.in.op)
    mulUnit.io.in <> io.in
    divUnit.io.in <> io.in
    io.out := Mux(op === MulDivUnitOp.DIV, divUnit.io.out, mulUnit.io.out)
    io.busy := mulUnit.io.busy | divUnit.io.busy
    forceName(io.in.src(0), "in_src0")
    forceName(io.in.src(1), "in_src1")
    forceName(io.in.op, "in_op")
    forceName(io.in.sign, "in_sign")
    forceName(io.out.res(0), "out_res0")
    forceName(io.out.res(1), "out_res1")
    forceName(io.busy, "out_busy")
}

object Generate_Mdu_rigid {
    def main(args: Array[String]): Unit = {
        println("The module is generating...")
        (new chisel3.stage.ChiselStage).execute(
            Array("--target-dir", "generated/MulDivUnit_rigid", "verilog"),
            Seq(ChiselGeneratorAnnotation(() => new MulDivUnit_rigid)))
    }
}
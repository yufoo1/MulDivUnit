import chisel3._
import chisel3.experimental.ChiselEnum
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.util._
import chisel3.util.experimental.forceName

object MulDivUnitOp extends ChiselEnum {
    val IDLE, MUL, DIV = Value
}

class MulDivUnitIn extends Bundle {
    val src = Vec(2, UInt(32.W))
    val op = MulDivUnitOp()
    val sign = Bool()
}

class MulDivUnitOut extends Bundle {
    val res = Vec(2, UInt(32.W))
}

class MulDivUnitIO extends Bundle {
    val in = Flipped(DecoupledIO(new MulDivUnitIn()))
    val out = DecoupledIO(new MulDivUnitOut())
}

class MulUnit extends Module {
    val io = IO(new MulDivUnitIO())
    val finish = RegInit(false.B)
    val tmp = RegInit(0.U(64.W))
    when(io.in.valid & io.in.ready & (io.in.bits.op === MulDivUnitOp.MUL)) {
        tmp := Mux(io.in.bits.sign, io.in.bits.src.map(i => i.asSInt).reduce(_ * _).asUInt, io.in.bits.src.reduce(_ * _))
        finish := true.B
    }.elsewhen(io.out.valid & io.out.ready) {
        tmp := 0.U
        finish := false.B
    }
    io.out.bits.res.zipWithIndex.foreach(i => i._1 := tmp(((i._2 + 1) << 5) - 1, i._2 << 5))
    io.in.ready := !finish
    io.out.valid := finish
}

class DivUnit extends Module {
    val io = IO(new MulDivUnitIO())
    val negSrcBits = io.in.bits.src.map(i => i(31) & io.in.bits.sign)
    val negResBits = Reg(Vec(2, Bool()))
    val absSrc = io.in.bits.src.zip(negSrcBits).map(i => Mux(i._2, -i._1, i._1))
    val absSrc64 = absSrc.zipWithIndex.map(i => Cat(0.U(32.W), i._1) << (i._2 << 5))
    val timer = RegInit(0.U(32.W))
    val tmps = Reg(Vec(4, UInt(67.W)))
    val subs = tmps.slice(1, 4).map(i => (tmps(0) << 2).asUInt - i)
    val tmp = Wire(Vec(2, UInt(32.W)))
    val busy = RegInit(false.B)
    tmp.zipWithIndex.foreach(i => i._1 := tmps(0)(((i._2 + 1) << 5) - 1, i._2 << 5))
    io.out.bits.res.zip(tmp.zip(negResBits)).foreach(i => i._1 := Mux(i._2._2, -i._2._1, i._2._1))
    io.in.ready := !busy
    io.out.valid := !timer(1) & busy
    when(io.in.valid & io.in.ready & io.in.bits.op === MulDivUnitOp.DIV) {
        timer := "hFFFFFFFF".U
        negResBits(0) := negSrcBits.reduce(_ ^ _)
        negResBits(1) := negSrcBits(0)
        tmps(0) := absSrc64(0)
        tmps(1) := absSrc64(1)
        tmps(2) := Cat(0.U(1.W), absSrc64(1)) << 1
        tmps(3) := (Cat(0.U(1.W), absSrc64(1)) << 1).asUInt + absSrc64(1)
        busy := true.B
    }.otherwise {
        when(io.out.valid & io.out.ready) {
            busy := false.B
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

class MulDivUnit extends Module {
    val io = IO(new MulDivUnitIO())
    val mulUnit = Module(new MulUnit)
    val divUnit = Module(new DivUnit)
    val op = RegNext(io.in.bits.op)
    mulUnit.io.in <> io.in
    divUnit.io.in <> io.in
    mulUnit.io.out.ready := io.out.ready
    divUnit.io.out.ready := io.out.ready
    io.in.ready := mulUnit.io.in.ready & divUnit.io.in.ready
    io.out.valid := mulUnit.io.out.valid | divUnit.io.out.valid
    io.out.bits := Mux(op === MulDivUnitOp.DIV, divUnit.io.out.bits, mulUnit.io.out.bits)
    forceName(io.in.ready, "in_ready")
    forceName(io.in.valid, "in_valid")
    forceName(io.out.ready, "out_ready")
    forceName(io.out.valid, "out_valid")
    forceName(io.in.bits.src(0), "in_src0")
    forceName(io.in.bits.src(1), "in_src1")
    forceName(io.in.bits.op, "in_op")
    forceName(io.in.bits.sign, "in_sign")
    forceName(io.out.bits.res(0), "out_res0")
    forceName(io.out.bits.res(1), "out_res1")
}

object Generate_Mdu {
    def main(args: Array[String]): Unit = {
        println("The module is generating...")
        (new chisel3.stage.ChiselStage).execute(
            Array("--target-dir", "generated/MulDivUnit", "verilog"),
            Seq(ChiselGeneratorAnnotation(() => new MulDivUnit)))
    }
}
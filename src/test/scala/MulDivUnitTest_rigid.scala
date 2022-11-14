import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MduTest_rigid extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "mdu_rigid"
    it should "do div" in test(new MulDivUnit_rigid).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        c.io.in.sign.poke(false)
        c.io.in.src(0).poke(0x444L)
        c.io.in.src(1).poke(0x444L)
        c.io.in.op.poke(MulDivUnitOp.DIV)
        c.clock.step(14)
        c.io.busy.expect(true)
        c.clock.step(2)
        c.io.busy.expect(false)
        c.io.out.res(0).expect(1)
    }

    it should "do mul" in test(new MulDivUnit_rigid).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        c.io.in.sign.poke(false)
        c.io.in.src(0).poke(0x444L)
        c.io.in.src(1).poke(0x444L)
        c.io.in.op.poke(MulDivUnitOp.MUL)
        c.clock.step(3)
        c.io.busy.expect(true)
        c.clock.step(3)
        c.io.busy.expect(false)
        c.io.out.res(0).expect(123210)
    }
}

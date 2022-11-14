import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MduTest extends AnyFlatSpec with ChiselScalatestTester {
    behavior of "mdu"
    it should "do div" in test(new MulDivUnit).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        c.io.in.bits.sign.poke(false)
        c.io.in.bits.src(0).poke(0x4L)
        c.io.in.bits.src(1).poke(0x3L)
        c.io.in.bits.op.poke(MulDivUnitOp.DIV)
        c.io.in.bits.sign.poke(true)
        c.io.in.valid.poke(true)
        c.io.out.valid.expect(false)
        c.clock.step()
        var cnt = 0
        while(!c.io.out.valid.peek().litToBoolean) {
            c.io.out.valid.expect(false)
            c.clock.step()
            cnt = cnt + 1
        }
        println(cnt)
        c.io.out.valid.expect(true)
        c.io.out.bits.res(0).expect(0x1)
        c.io.out.bits.res(1).expect(0x1)
        c.clock.step(10)
        c.io.out.ready.poke(true)
        c.io.out.valid.expect(true)
        c.clock.step()
        c.io.out.valid.expect(false)
    }

    it should "do mul" in test(new MulDivUnit).withAnnotations(Seq(WriteVcdAnnotation)) { c =>
        c.io.in.bits.sign.poke(false)
        c.io.in.bits.src(0).poke(0x444L)
        c.io.in.bits.src(1).poke(0x444L)
        c.io.in.bits.op.poke(MulDivUnitOp.MUL)
        c.io.in.bits.sign.poke(false)
        c.io.in.valid.poke(true)
        c.io.out.valid.expect(false)
        c.clock.step()
        c.io.out.valid.expect(true)
        c.io.out.bits.res(0).expect(0x123210)
        c.io.out.bits.res(1).expect(0x0)
        c.io.out.valid.expect(true)
        c.io.in.bits.src(0).poke(0x333L)
        c.io.in.bits.src(1).poke(0x333L)
        c.io.in.ready.expect(false)
        c.clock.step()
        c.io.in.ready.expect(false)
        c.io.out.valid.expect(true)
        c.clock.step()
        c.io.out.ready.poke(true)
        c.io.out.valid.expect(true)
        c.io.out.bits.res(0).expect(0x123210)
        c.io.out.bits.res(1).expect(0x0)
        c.io.in.ready.expect(false)
        c.clock.step()
        c.io.in.ready.expect(true)
        c.io.in.valid.expect(true)
        c.clock.step()
        c.io.out.bits.res(0).expect(0xa3c29)
    }
}

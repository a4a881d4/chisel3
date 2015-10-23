// See LICENSE for license details.

package chiselTests
import Chisel._
import org.scalatest._
import org.scalatest.prop._
import org.scalacheck._
import Chisel.testers.BasicTester

class Decoder(bitpats: List[String]) extends Module {
  val io = new Bundle {
    val inst  = UInt(INPUT, 32)
    val matched = Bool(OUTPUT)
  }
  io.matched := Vec(bitpats.map(BitPat(_) === io.inst)).reduce(_||_)
}

class DecoderSpec extends ChiselPropSpec {

  class DecoderTester(pairs: List[(String, String)]) extends BasicTester {
    val (insts, bitpats) = pairs.unzip
    val (cnt, wrap) = Counter(Bool(true), pairs.size)
    val dut = Module(new Decoder(bitpats))
    dut.io.inst := Vec(insts.map(UInt(_)))(cnt)
    when(!dut.io.matched) { io.done := Bool(true); io.error := cnt }
    when(wrap) { io.done := Bool(true) }
  }

  // Use a single Int to make both a specific instruction and a BitPat that will match it
  val bitpatPair = for(seed <- Arbitrary.arbitrary[Int]) yield {
    val rnd = new scala.util.Random(seed)
    val bs = seed.toBinaryString
    val bp = bs.map(if(rnd.nextBoolean) _ else "?").mkString
    ("b" + bs, "b" + bp)
  }
  def nPairs(n: Int) = Gen.containerOfN[List, (String,String)](n,bitpatPair)

  property("BitPat wildcards should be usable in decoding") {
    forAll(nPairs(16)){ (pairs: List[(String, String)]) =>
      assert(execute{ new DecoderTester(pairs) })
    }
  }
}
package lms.parallel

import lms.core._
import lms.util._
import lms.core.stub._
import lms.core.Backend._
import lms.core.virtualize
import lms.core.utils.time
import lms.macros.SourceContext
import lms.collection.immutable._
import lms.collection.mutable._

abstract class ParallelSnippet[A: Manifest, B: Manifest] extends SPOps {
  def wrapper(x: Rep[A]): Rep[B] = snippet(x)
  def snippet(x: Rep[A]): Rep[B]
}


trait SPOps extends Base 
  with PrimitiveOps with LiftPrimitives with Equal with RangeOps
  with OrderingOps  with LiftVariables  with TupleOps
  with ListOpsOpt   with MapOpsOpt      with SetOpsOpt 
  with ArrayOps     with MPIOps {
  override def __fun[T: Manifest](f: AnyRef, arity: Int, gf: List[Backend.Exp] => Backend.Exp, captures: Backend.Exp*): Backend.Exp = {
    // No λforward
    val can = canonicalize(f)
    Adapter.funTable.find(_._2 == can) match {
      case Some((funSym, _)) => funSym
      case _ =>
        val fn = Backend.Sym(Adapter.g.fresh)
        Adapter.funTable = (fn, can) :: Adapter.funTable
        val block = Adapter.g.reify(arity, gf)
        val res = Adapter.g.reflect(fn, "λ", (block+:captures):_*)(hardSummary(fn))
        Adapter.funTable = Adapter.funTable.map {
          case (fn2, can2) => if (can == can2) (fn, can) else (fn2, can2)
        }
        res
    }
  }

}
package lms.collection.immutable

import lms.core._
import lms.util._
import lms.core.stub._
import lms.core.Backend._
import lms.core.virtualize
import lms.core.utils.time
import lms.macros.SourceContext

trait ListOps { b: Base =>
  object List {
    def apply[A: Manifest](xs: Rep[A]*)(implicit pos: SourceContext) = {
      val mA = Backend.Const(manifest[A])
      val unwrapped_xs = Seq(mA) ++ xs.map(Unwrap)
      Wrap[List[A]](Adapter.g.reflect("list-new", unwrapped_xs:_*))
    }
  }

  implicit def __liftConstList[A: Manifest](xs: List[A]): ListOps[A] = new ListOps(unit(xs))
  implicit def __liftVarList[A: Manifest](xs: Var[List[A]]): ListOps[A] = new ListOps(readVar(xs))

  implicit class ListOps[A: Manifest](xs: Rep[List[A]]) {
    def apply(i: Rep[Int]): Rep[A] = Wrap[A](Adapter.g.reflect("list-apply", Unwrap(xs), Unwrap(i)))
    def head: Rep[A] = Wrap[A](Adapter.g.reflect("list-head", Unwrap(xs)))
    def tail: Rep[List[A]] = Wrap[List[A]](Adapter.g.reflect("list-tail", Unwrap(xs)))
    def size: Rep[Int] = Wrap[Int](Adapter.g.reflect("list-size", Unwrap(xs)))
    def isEmpty: Rep[Boolean] = Wrap[Boolean](Adapter.g.reflect("list-isEmpty", Unwrap(xs)))
    def take(i: Rep[Int]) = Wrap[List[A]](Adapter.g.reflect("list-take", Unwrap(xs), Unwrap(i)))
    def ::(x: Rep[A]): Rep[List[A]] =
      Wrap[List[A]](Adapter.g.reflect("list-prepend", Unwrap(xs), Unwrap(x)))
    def ++(ys: Rep[List[A]]): Rep[List[A]] =
      Wrap[List[A]](Adapter.g.reflect("list-concat", Unwrap(xs), Unwrap(ys)))
    def mkString: Rep[String] = mkString(unit(""))
    def mkString(sep: Rep[String]): Rep[String] =
      Wrap[String](Adapter.g.reflect("list-mkString", Unwrap(xs), Unwrap(sep)))
    def toArray: Rep[Array[A]] = Wrap[Array[A]](Adapter.g.reflect("list-toArray", Unwrap(xs)))
    def toSeq: Rep[Seq[A]] = Wrap[Seq[A]](Adapter.g.reflect("list-toSeq", Unwrap(xs)))
    def map[B: Manifest](f: Rep[A] => Rep[B]): Rep[List[B]] = {
      val block = Adapter.g.reify(x => Unwrap(f(Wrap[A](x))))
      Wrap[List[B]](Adapter.g.reflect("list-map", Unwrap(xs), block))
    }
    def flatMap[B: Manifest](f: Rep[A] => Rep[List[B]]): Rep[List[B]] = {
      val block = Adapter.g.reify(x => Unwrap(f(Wrap[A](x))))
      val mA = Backend.Const(manifest[A])
      Wrap[List[B]](Adapter.g.reflect("list-flatMap", Unwrap(xs), block, mA))
    }
    def foldLeft[B: Manifest](z: Rep[B])(f: (Rep[B], Rep[A]) => Rep[B]): Rep[B] = {
      val block = Adapter.g.reify((x, y) => Unwrap(f(Wrap[B](x), Wrap[A](y))))
      Wrap[B](Adapter.g.reflect("list-foldLeft", Unwrap(xs), Unwrap(z), block))
    }
    def zip[B: Manifest](ys: Rep[List[B]]): Rep[List[(A, B)]] =
      Wrap[List[(A, B)]](Adapter.g.reflect("list-zip", Unwrap(xs), Unwrap(ys)))
    def filter(f: Rep[A] => Rep[Boolean]): Rep[List[A]] = {
      val block = Adapter.g.reify(x => Unwrap(f(Wrap[A](x))))
      Wrap[List[A]](Adapter.g.reflect("list-filter", Unwrap(xs), block))
    }
    def withFilter(f: Rep[A] => Rep[Boolean]): Rep[List[A]] = filter(f)
    def sortBy[B: Manifest : Ordering](f: Rep[A] => Rep[B]): Rep[List[A]] = {
      val block = Adapter.g.reify(x => Unwrap(f(Wrap[A](x))))
      Wrap[List[A]](Adapter.g.reflect("list-sortBy", Unwrap(xs), block))
    }
    def containsSlice[B <: A : Manifest](ys: Rep[List[B]]): Rep[Boolean] =
      Wrap[Boolean](Adapter.g.reflect("list-containsSlice", Unwrap(xs), Unwrap(ys)))
    def intersect[B >: A : Manifest](ys: Rep[List[B]]): Rep[List[A]] =
      Wrap[List[A]](Adapter.g.reflect("list-intersect", Unwrap(xs), Unwrap(ys)))
  }
}

trait ListOpsOpt extends ListOps { b: Base =>
  implicit override def __liftConstList[A: Manifest](xs: List[A]): ListOps[A] = new ListOpsOpt(unit(xs))
  implicit override def __liftVarList[A: Manifest](xs: Var[List[A]]): ListOps[A] = new ListOpsOpt(readVar(xs))

  implicit class ListOpsOpt[A: Manifest](xs: Rep[List[A]]) extends ListOps[A](xs) {
    override def ++(ys: Rep[List[A]]): Rep[List[A]] = (Unwrap(xs), Unwrap(ys)) match {
      case (Adapter.g.Def("list-new", mA::(xs: List[Backend.Exp])),
            Adapter.g.Def("list-new",  _::(ys: List[Backend.Exp]))) =>
        val unwrapped_xsys = Seq(mA) ++ xs ++ ys
        Wrap[List[A]](Adapter.g.reflect("list-new", unwrapped_xsys:_*))
      case (Adapter.g.Def("list-new", mA::(xs: List[Backend.Exp])), _) if xs.isEmpty =>
        ys
      case (_, Adapter.g.Def("list-new", mA::(ys: List[Backend.Exp]))) if ys.isEmpty =>
        xs
      case _ => super.++(ys)
    }
    override def foldLeft[B: Manifest](z: Rep[B])(f: (Rep[B], Rep[A]) => Rep[B]): Rep[B] =
      Unwrap(xs) match {
        case Adapter.g.Def("list-new", mA::(xs: List[Backend.Exp])) =>
          xs.map(Wrap[A](_)).foldLeft(z)(f)
        case _ => super.foldLeft(z)(f)
      }
  }
}

trait ScalaCodeGen_List extends ExtendedScalaCodeGen {
  override def remap(m: Manifest[_]): String = {
    if (m.runtimeClass.getName == "scala.collection.immutable.List") {
      val kty = m.typeArguments(0)
      s"List[${remap(kty)}]"
    } else { super.remap(m) }
  }

  override def mayInline(n: Node): Boolean = n match {
    case Node(_, "list-new", _, _) => false
    case Node(_, "list-map", _, _) => false
    case Node(_, "list-flatMap", _, _) => false
    case Node(_, "list-foldLeft", _, _) => false
    case Node(_, "list-take", _, _) => false
    case Node(_, "list-prepend", _, _) => false
    case Node(_, "list-concat", _, _) => false
    case Node(_, "list-zip", _, _) => false
    case Node(_, "list-sortBy", _, _) => false
    case _ => super.mayInline(n)
  }

  override def quote(s: Def): String = s match {
    case Const(xs: List[_]) =>
      "List(" + xs.map(x => quote(Const(x))).mkString(", ") + ")"
    case _ => super.quote(s)
  }

  override def shallow(n: Node): Unit = n match {
    case Node(s, "list-new", Const(mA: Manifest[_])::xs, _) =>
      val ty = remap(mA)
      es"List[$ty]("
      xs.zipWithIndex.map { case (x, i) =>
        shallow(x)
        if (i != xs.length-1) emit(", ")
      }
      emit(")")
    case Node(s, "list-apply", List(xs, i), _) => es"$xs($i)"
    case Node(s, "list-head", List(xs), _) => es"$xs.head"
    case Node(s, "list-tail", List(xs), _) => es"$xs.tail"
    case Node(s, "list-size", List(xs), _) => es"$xs.size"
    case Node(s, "list-isEmpty", List(xs), _) => es"$xs.isEmpty"
    case Node(s, "list-take", List(xs, i), _) => es"$xs.take($i)"
    case Node(s, "list-prepend", List(xs, x), _) => es"$x :: $xs"
    case Node(s, "list-concat", List(xs, ys), _) => es"$xs ++ $ys"
    case Node(s, "list-mkString", List(xs, Const("")), _) => es"$xs.mkString"
    case Node(s, "list-mkString", List(xs, sep), _) => es"$xs.mkString($sep)"
    case Node(s, "list-toArray", List(xs), _) => es"$xs.toArray"
    case Node(s, "list-toSeq", List(xs), _) => es"$xs.toSeq"
    case Node(s, "list-map", List(xs, b), _) => es"$xs.map($b)"
    case Node(s, "list-flatMap", xs::(b: Block)::rest, _) => es"$xs.flatMap($b)"
    case Node(s, "list-foldLeft", List(xs, z, b), _) =>
      es"$xs.foldLeft($z)("; shallow(b, false); emit(")")
    case Node(s, "list-zip", List(xs, ys), _) => es"$xs.zip($ys)"
    case Node(s, "list-filter", List(xs, b), _) => es"$xs.filter($b)"
    case Node(s, "list-sortBy", List(xs, b), _) => es"$xs.sortBy($b)"
    case Node(s, "list-containsSlice", List(xs, ys), _) => es"$xs.containsSlice($ys)"
    case Node(s, "list-intersect", List(xs, ys), _) => es"$xs.intersect($ys)"
    case _ => super.shallow(n)
  }

  //TODO: what should be added here?
  override def traverse(n: Node): Unit = n match {
    case _ => super.traverse(n)
  }

  //TODO: what should be added here?
  override def symsFreq(n: Node): Set[(Def, Double)] = n match {
    case _ => super.symsFreq(n)
  }
}

// A List codegen using the immer library
trait CppCodeGen_List extends ExtendedCPPCodeGen {
  registerHeader("<immer/immer/flex_vector.hpp>")
  registerHeader("<immer/immer/algorithm.hpp>")
  registerHeader("<immer_contrib.hpp>")

  override def remap(m: Manifest[_]): String = {
    if (m.runtimeClass.getName == "scala.collection.immutable.List") {
      val kty = m.typeArguments(0)
      s"immer::flex_vector<${remap(kty)}>"
    } else { super.remap(m) }
  }

  override def mayInline(n: Node): Boolean = n match {
    case Node(_, name, _, _) if name.startsWith("list-") => false
    case _ => super.mayInline(n)
  }

  override def quote(s: Def): String = s match {
    case Const(xs: List[_]) =>
      "{" + xs.map(x => quote(Const(x))).mkString(", ") + "}"
    case _ => super.quote(s)
  }

  override def shallow(n: Node): Unit = n match {
    case Node(s, "list-new", Const(mA: Manifest[_])::xs, _) =>
      emit(s"immer::flex_vector<${remap(mA)}>{")
      xs.zipWithIndex.map { case (x, i) =>
        shallow(x)
        if (i != xs.length-1) emit(", ")
      }
      emit("}")
    case Node(s, "list-apply", List(xs, i), _) => es"$xs.at($i)"
    case Node(s, "list-head", List(xs), _) => es"$xs.front()"
    case Node(s, "list-tail", List(xs), _) => es"$xs.drop(1)"
    case Node(s, "list-size", List(xs), _) => es"$xs.size()"
    case Node(s, "list-isEmpty", List(xs), _) => es"$xs.size() == 0"
    case Node(s, "list-take", List(xs, i), _) => es"$xs.take($i)"
    case Node(s, "list-prepend", List(xs, x), _) => es"$x.push_front($xs)"
    case Node(s, "list-concat", List(xs, ys), _) => es"$xs + $ys"
    case Node(s, "list-map", List(xs, b: Block), _) =>
      val retType = remap(typeBlockRes(b.res))
      es"ImmerContrib::vmap<$retType>($xs, $b)"
    case Node(s, "list-flatMap", xs::(b: Block)::Const(mA: Manifest[_])::rest, _) =>
      // Note: b.res must return a List type, as required by flatMap
      val retType = remap(typeBlockRes(b.res).typeArguments(0))
      es"ImmerContrib::flatMap<$retType>($xs, $b)"
    case Node(s, "list-foldLeft", List(xs, z, b), _) =>
      es"ImmerContrib::foldLeft($xs, $z, $b)"
    case Node(s, "list-zip", List(xs, ys), _) =>
      es"ImmerContrib::zip($xs, $ys)"
    case Node(s, "list-filter", List(xs, b), _) =>
      es"ImmerContrib::filter($xs, $b)"
    // Unsupported operation for immer backend
    case Node(s, "list-mkString", List(xs, sep), _) =>
      ???
    case Node(s, "list-toArray", List(xs), _) =>
      ???
    case Node(s, "list-toSeq", List(xs), _) =>
      ???
    case Node(s, "list-sortBy", List(xs, b), _) =>
      ???
    case Node(s, "list-containsSlice", List(xs, ys), _) =>
      ???
    case Node(s, "list-intersect", List(xs, ys), _) =>
      ???
    case _ => super.shallow(n)
  }
}


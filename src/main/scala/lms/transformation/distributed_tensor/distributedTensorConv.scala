package lms.transformation.tensor

import scala.annotation.implicitNotFound
import scala.collection._

import lms.core._
import lms.core.stub._
import lms.collection.mutable._
import lms.macros.SourceContext
import lms.thirdparty.array_computation.{ArrayCPUOps, CUDATypeLess, CudaOps}
import lms.thirdparty.{CUDNNTypeLess, CUDNNOps}

import Backend._


trait FixedSizeDistributedTensorConvTypeLess extends FixedSizeDistributedTensorMutationTypeLess {

  def ConvBackwardData(weight: TENSOR, filter: TENSOR, anno: Anno, __pos: SourceContext): TENSOR = {
    assert(weight.et == filter.et)
    val res_tt = weight.tensor_type
    (new TENSOR(Adapter.g.reflectRead("tensor_conv_bwd_data", C(res_tt), C(anno), 
      weight.x, filter.x)(weight.x, filter.x)).withSrcType(__pos, weight.et))
  }

  def ConvBackwardFilter(weight: TENSOR, filter: TENSOR, anno: Anno, __pos: SourceContext): TENSOR = {
    assert(weight.et == filter.et)
    val res_tt = filter.tensor_type
    (new TENSOR(Adapter.g.reflectRead("tensor_conv_bwd_filter", C(res_tt), C(anno), 
      weight.x, filter.x)(weight.x, filter.x)).withSrcType(__pos, weight.et))
  }

  override def aircopCollect(node: Node, forwardNodes: mutable.ArrayBuffer[Node],
    weightNodes: mutable.ArrayBuffer[Node], backwardNodes: mutable.ArrayBuffer[()=>Unit],
    gradMap: mutable.HashMap[Backend.Sym, TENSOR],
    momentumMap: mutable.HashMap[Backend.Sym, TENSOR],
    transform: Backend.Exp => Backend.Exp) = node match {
      case Node(s, "tensor_conv", tt::Backend.Const(anno:Anno)::(a:Backend.Sym)::(b:Backend.Sym)::Backend.Const(padding:Seq[Int])::Backend.Const(strides:Seq[Int])::Backend.Const(dilation:Seq[Int])::
        Backend.Const(alpha:Float)::Backend.Const(beta:Float)::_, _) =>
        implicit val pos = Adapter.oldSourceMap(s)
        // save forward op in forwardNodes
        forwardNodes += node
        // save backward op in backwardNodes
        val x = new TENSOR(transform(a))
        val y = new TENSOR(transform(b))

        (() => {
          val a_grad = ConvBackwardData(x, y, anno, pos)
          Accumulate(gradMap(a), a_grad, anno); ()
        }) +=: backwardNodes
        (() => {
          val b_grad = ConvBackwardFilter(x, y, anno, pos)
          Accumulate(gradMap(b), b_grad, anno); ()
        }) +=: backwardNodes

      case _ => super.aircopCollect(node, forwardNodes, weightNodes, backwardNodes, gradMap, momentumMap, transform)
    }
}

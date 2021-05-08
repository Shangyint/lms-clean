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
import lms.thirdparty.CCodeGenLibs


trait SPCodeGen extends ExtendedCCodeGen with CCodeGenMPI with CCodeGenLibs {

}
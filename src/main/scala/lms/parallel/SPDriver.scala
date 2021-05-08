package lms.parallel

import lms.core._
import lms.core.stub._
import lms.core.Backend._
import lms.core.utils.time
import lms.core.virtualize
import lms.macros.SourceContext

abstract class SPDriver[A: Manifest, B: Manifest](appName: String, folder: String = ".") 
  extends ParallelSnippet[A, B] with SPOps { q =>

  import java.io.{File, PrintStream}

  val codegen = new SPCodeGen {
    val IR: q.type = q
    val codegenFolder = s"$folder/$appName/"
  }

  def createNewDir: Boolean = {
    val codegenFolderFile = new File(codegen.codegenFolder)
    if (!codegenFolderFile.exists()) codegenFolderFile.mkdir
    else {
      val entries = codegenFolderFile.list()
      entries.map(x => {
        if (x == "build") {
          val build_dir = new File(codegenFolderFile.getPath, x)
          build_dir.list.map(x => new File(build_dir.getPath, x).delete)
          build_dir.delete
        }
        else new File(codegenFolderFile.getPath, x).delete
      })
      codegenFolderFile.delete
      codegenFolderFile.mkdir
    }
  }

  def genSource: Unit = {
    val folderFile = new File(folder)
    if (!folderFile.exists()) folderFile.mkdir
    createNewDir
    val mainStream = new PrintStream(s"$folder/$appName/$appName.c")
    val statics = Adapter.emitCommon1(appName, codegen, mainStream)(manifest[A], manifest[B])(x => Unwrap(wrapper(Wrap[A](x))))
    mainStream.close
  }

  def genMakeFile: Unit = {
    val out = new PrintStream(s"$folder/$appName/Makefile")
    val libraries = codegen.libraryFlags.mkString(" ")
    val includes = codegen.includePaths.map(s"-I " + _).mkString(" ")
    // val libraryPaths = codegen.libraryPaths.map(s"-L " + _).mkString(" ")

    out.println(s"""|INCLUDES = $includes
    |
    |all: $appName
    |$appName: $appName.c
    |\tmpicc -o $appName $appName.c $$(LDFLAGS) $$(INCLUDES)
    |
    |clean:
    |\t@rm $appName 2>/dev/null || true
    |""".stripMargin)
    out.close
  }

  def genAll: Unit = {
    genSource
    genMakeFile
  }
}


object TestSPSendRecv {
  def specialize(name: String): SPDriver[Int, Unit] = 
    new SPDriver[Int, Unit](name, "./sp_gen") {
      @virtualize
      def snippet(x: Rep[Int]): Rep[Unit] = {
        mpi_init()

        var world_size = 0
        mpi_comm_size(mpi_comm_world, world_size)

        var world_rank = 0
        mpi_comm_rank(mpi_comm_world, world_rank)

        val buffer = NewArray[Int](10)

        if (world_rank == 0) {
          for (i <- (0 until 10): Rep[Range]) {
            buffer(i) = i
          }
          MPI_CHECK(mpi_send(buffer, 10, mpi_int, 1, 123, mpi_comm_world))
        } else {
          var status = mpi_status_new
          MPI_CHECK(mpi_recv(buffer, 10, mpi_int, 0, 123, mpi_comm_world, status))
          for (i <- (0 until 10): Rep[Range]) {
            if (buffer(i) != i) 
              printf("Error: buffer[%d] = %d, but expected %d\n", i, buffer(i), i)
          }
        }

        mpi_finalize()
        ()
      }
    }

  def main(args: Array[String]): Unit = {
    val code = specialize("testSendRecv").genAll
  }
}

object TestAdd {
  def specialize(name: String): SPDriver[Int, Unit] = 
    new SPDriver[Int, Unit](name, "./sp_gen") {
    // c code at http://condor.cc.ku.edu/~grobe/docs/intro-MPI-C.shtml
      @virtualize
      def snippet(x: Rep[Int]): Rep[Unit] = {
        val rows = x
        val send_tag = 2001
        val return_tag = 2002
        var rows_to_send = 0
        var sum = 0
        var partial_sum = 0
        val status = mpi_status_new

        val array = NewArray[Int](rows)
        val array2 = NewArray[Int](rows)

        mpi_init()

        var world_size = 0
        mpi_comm_size(mpi_comm_world, world_size)

        var world_rank = 0
        mpi_comm_rank(mpi_comm_world, world_rank)

        if (world_rank == 0) {
          val row_proc = rows / world_size
          for (i <- (0 until rows): Rep[Range]) {
            array(i) = i + 1
          }

          for (id <- (1 until world_size): Rep[Range]) {
            val start = id*row_proc+1;
            var end = (id+1)*row_proc
            if (rows - (end + 1) < row_proc) end = rows - 1
            rows_to_send = end - start + 1
            MPI_CHECK(mpi_send_T(rows_to_send, 1, mpi_int, id, send_tag, mpi_comm_world))
            MPI_CHECK(mpi_send(array + start, rows_to_send, mpi_int, id, send_tag, mpi_comm_world))
          }

          sum = 0
          for (i <- (0 until row_proc + 1): Rep[Range]) {
            sum += array(i)
          }

          for (i <- (1 until world_size): Rep[Range]) {
            MPI_CHECK(mpi_recv_T(partial_sum, 1, mpi_int, mpi_any_src, return_tag, mpi_comm_world, status))
            sum += partial_sum
          }

          printf("Sum is %d", sum);
        } else {
          var rows_to_recv = rows / world_size
          MPI_CHECK(mpi_recv_T(rows_to_recv, 1, mpi_int, 0, send_tag, mpi_comm_world, status))
          MPI_CHECK(mpi_recv(array2, rows_to_recv, mpi_int, 0, send_tag, mpi_comm_world, status))

          partial_sum = 0
          for (i <- (0 until rows_to_recv): Rep[Range]) {
            partial_sum += array2(i)
          }

          MPI_CHECK(mpi_send_T(partial_sum, 1, mpi_int, 0, return_tag, mpi_comm_world))
        }
        mpi_finalize()
        ()
      }
    }

  def main(args: Array[String]): Unit = {
    val code = specialize("testAdd").genAll
  }
}
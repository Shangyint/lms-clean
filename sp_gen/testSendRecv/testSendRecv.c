
/*****************************************
Emitting C Generated Code
*******************************************/
    
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include "mpi_header.h"

/**************** testSendRecv ****************/
void testSendRecv(int x0) {
MPI_Init(NULL, NULL);
int x1 = 0;
MPI_Comm_rank(MPI_COMM_WORLD, &x1);
int* x2 = (int*)malloc(10 * sizeof(int));
if (x1 == 0) {
int x3 = 0;
while (x3 != 10) {
int x4 = x3;
x2[x4] = x4;
x3 = x3 + 1;
}
MPICHECK(MPI_Send(x2, 10, MPI_INT, 1, 123, MPI_COMM_WORLD));
} else {
MPI_Status x5;
MPICHECK(MPI_Recv(x2, 10, MPI_INT, 0, 123, MPI_COMM_WORLD, &x5));
int x6 = 0;
while (x6 != 10) {
int x7 = x6;
if (x2[x7] != x7) printf("Error: buffer[%d] = %d, but expected %d\n", x7, x2[x7], x7);
x6 = x6 + 1;
}
}
MPI_Finalize();
}

/*****************************************
End of C Generated Code
*******************************************/
int main(int argc, char *argv[]) {
  if (argc != 2) {
    printf("usage: %s <arg>\n", argv[0]);
    return 0;
  }
  testSendRecv(atoi(argv[1]));
  return 0;
}


/*****************************************
Emitting C Generated Code
*******************************************/
    
#include <stdlib.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>
#include "mpi_header.h"

/**************** testAdd ****************/
void testAdd(int x0) {
int x1 = 0;
int x2 = 0;
int* x3 = (int*)malloc(x0 * sizeof(int));
int* x4 = (int*)malloc(x0 * sizeof(int));
MPI_Init(NULL, NULL);
int x5 = 0;
MPI_Comm_size(MPI_COMM_WORLD, &x5);
int x6 = 0;
MPI_Comm_rank(MPI_COMM_WORLD, &x6);
if (x6 == 0) {
MPI_Status x7;
int x8 = x0 / x5;
int x9 = 0;
while (x9 != x0) {
int x10 = x9;
x3[x10] = x10 + 1;
x9 = x9 + 1;
}
int x11 = x5;
int x12 = 1;
int x13 = x0 - 1;
while (x12 != x11) {
int x14 = x12;
int x15 = x14 * x8 + 1;
int x16 = (x14 + 1) * x8;
int x17 = x16;
if (x0 - (x16 + 1) < x8) x17 = x13;
int x18 = x17 - x15 + 1;
MPICHECK(MPI_Send(&x18, 1, MPI_INT, x14, 2001, MPI_COMM_WORLD));
MPICHECK(MPI_Send(&(x3[x15]), x18, MPI_INT, x14, 2001, MPI_COMM_WORLD));
x12 = x12 + 1;
}
x1 = 0;
int x19 = x8 + 1;
int x20 = 0;
while (x20 != x19) {
x1 = x1 + x3[x20];
x20 = x20 + 1;
}
int x21 = x5;
int x22 = 1;
while (x22 != x21) {
MPICHECK(MPI_Recv(&x2, 1, MPI_INT, MPI_ANY_SOURCE, 2002, MPI_COMM_WORLD, &x7));
x1 = x1 + x2;
x22 = x22 + 1;
}
printf("Sum is %d", x1);
} else {
MPI_Status x7;
int x23 = x0 / x5;
MPICHECK(MPI_Recv(&x23, 1, MPI_INT, 0, 2001, MPI_COMM_WORLD, &x7));
MPICHECK(MPI_Recv(x4, x23, MPI_INT, 0, 2001, MPI_COMM_WORLD, &x7));
x2 = 0;
int x24 = 0;
while (x24 != x23) {
x2 = x2 + x4[x24];
x24 = x24 + 1;
}
MPICHECK(MPI_Send(&x2, 1, MPI_INT, 0, 2002, MPI_COMM_WORLD));
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
  testAdd(atoi(argv[1]));
  return 0;
}

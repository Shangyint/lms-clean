INCLUDES = -I /home/shangyint/research/lms-clean/headers

all: testSendRecv
testSendRecv: testSendRecv.c
	mpicc -o testSendRecv testSendRecv.c $(LDFLAGS) $(INCLUDES)

clean:
	@rm testSendRecv 2>/dev/null || true


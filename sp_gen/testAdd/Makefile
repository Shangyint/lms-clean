INCLUDES = -I /home/shangyint/research/lms-clean/headers

all: testAdd
testAdd: testAdd.c
	mpicc -o testAdd testAdd.c $(LDFLAGS) $(INCLUDES)

clean:
	@rm testAdd 2>/dev/null || true


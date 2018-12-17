# Makefile template for Static library.
# 1. Compile every *.c file in the folder
# 2. All obj files under obj folder
# 3. static library .a at lib folder
# 4. run 'make dirmake' before calling 'make'
CFLAGS= -fPIC -O0 -g -Wall -c -fpermissive -mmacosx-version-min=10.11
INC=
OUT_FILE_NAME = liblinenoise.a
SRC_DIR=./src/linenoise
OBJ_DIR=./build/linenoise/o
OUT_DIR=./build/linenoise
$(OUT_FILE_NAME): $(OBJ_DIR)/linenoise.o
	ar ru $(OUT_DIR)/$@ $^
	ranlib $(OUT_DIR)/$@
$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c dirmake
	$(CC) -c $(INC) $(CFLAGS) -o $@  $<
dirmake:
	@mkdir -p $(OUT_DIR)
	@mkdir -p $(OBJ_DIR)
clean:
	rm -f $(OBJ_DIR)/*.o $(OUT_DIR)/$(OUT_FILE_NAME) Makefile.bak
rebuild: clean build

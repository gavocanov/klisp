# Specify extensions of files to delete when cleaning
CLEANEXTS   = o a

# Specify the target file and the install directory
OUTPUTFILE  = liblinenoise.a
INSTALLDIR  = ../../build/linenoise

# Default target
.PHONY: all
all: $(OUTPUTFILE)

# Build lib
$(OUTPUTFILE): linenoise.o
	ar ru $@ $^
	ranlib $@

.PHONY: install
install:
	mkdir -p $(INSTALLDIR)
	cp -p $(OUTPUTFILE) $(INSTALLDIR)

.PHONY: clean
clean:
	for file in $(CLEANEXTS); do rm -f *.$$file; done

# Indicate dependencies of .ccp files on .hpp files
linenoise.o: linenoise.h

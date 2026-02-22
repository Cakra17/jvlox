.PHONY: build run clean

outdir=src/

build:
	javac -d bin/ src/*.java

build-ast:
	javac -d bin/ tool/GenerateAst.java

run:
	@if [ -z "$(FILEPATH)" ]; then \
		java -cp bin/ src.Jvlox; \
	else \
		java -cp bin/ src.Jvlox $(FILEPATH); \
	fi

generate-ast:
	java -cp bin/ tool.GenerateAst $(outdir)

clean:
	rm -rf bin/

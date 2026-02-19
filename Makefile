.PHONY: build run clean

build:
	javac -d bin/ src/*.java

run:
	@if [ -z "$(FILEPATH)" ]; then \
		java -cp bin/ src.Jvlox; \
	else \
		java -cp bin/ src.Jvlox $(FILEPATH); \
	fi

clean:
	rm -rf bin/

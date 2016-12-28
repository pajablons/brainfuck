/***************************************************************************
 * @author Peter Jablonski
 * Compiler.java
 * 
 * A trivial, java-based Brainfuck interpreter/compiler thing (.bf to .class)
 * For debugging purposes, the translation is provided in bf.java.
 *
 * A few notes:
 * - In the event that bf.class or bf.java exists, this will overwrite them.
 *   Don't be an idiot.
 * - Error checking is provided only for potential compilation problems, which
 *   means we check that there are no unmatched square braces.
 * - Error checking is NOT provided for logic errors, such as moving the mem
 *   pointer left of 0 or right of 30000.
 * - Memory cells are 8 bit bytes.  In the event of expected overflows, one
 *   solution may be to use multiple cells to simulate a larger cell size.
 * - IO is performed in integer-represented bytes.  This means that "a" is 
 *   not accepted, but 97 is.  
 * - Bytes are signed, with the valid range of [-128,127]
 *
 * This program makes no promises of safety.  Again, don't be an idiot.
 ***************************************************************************/

import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.PrintWriter;
import java.lang.Runtime;
import java.lang.Process;

public class Compiler {
	public static boolean validateArgs(String[] args) {
		if(args.length < 1) {
			System.err.println("Invalid argument count.");
			System.err.println("Proper Usage:");
			System.err.println("\t$ java Compiler <source> [outfile]");
			return false;
		}
		return true;
	}
	public static boolean sourceExists(String fname) {
		File src = new File(fname);
		return src.exists();
	}
	public static String getOutputFile(String[] args) {
		if(args.length == 2) {
			return args[1];
		} else {
			return "bf.java";
		}
	}
	public static String readFile(String fname) {
		String source = "";
		try {
			Reader reader = new FileReader(new File(fname));
			int next;
			while((next = reader.read()) != -1) {
				source += (char)next;
			}
			reader.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return source;
	}
	public static String stripFormat(String baseSource) {
		// A fun note here:
		// We use "\\-" in order to pass the literal '-'.
		// '-' is a special character in regexes, so we escape it as '\-'
		// However, java interprets this similarly to \n and pitches an error,
		// so we pass a second \ to escape that first escape in the string.
		// Java interprets (and sends) "\\-" as the literal string '\-'
		// which the regex engine reads as the literal '-'.
		// This is why modern languages distinguish '' from "",
		// with the former being a literal string and the latter being
		// a formatting string.
		String regex = "[^><+\\-.,\\[\\]]";
		return baseSource.replaceAll(regex,"");
	}
	public static String getTranslation(char token) {
		switch(token) {
			case '>':	return "ptr++;\n";
			case '<':	return "ptr--;\n";
			case '+':	return "mem[ptr]++;\n";
			case '-':	return "mem[ptr]--;\n";
			case '.':	return "System.out.print(mem[ptr]);\n";
			case ',':	return "mem[ptr] = iostream.nextByte();\n";
			case '[':	return "while(mem[ptr] != 0) {\n";
			case ']':	return "}\n";
		}
		// unreachable, given that we've stripped all other characters
		return "";
	}
	public static String translate(String source) {
		String out = "";
		int braceCount = 0;
		try {
			Reader reader = new StringReader(source);
			int next;
			while((next = reader.read()) != -1 &&
			      braceCount >= 0) {
				out += getTranslation((char)next);
				if(next == '[') {
					braceCount++;
				} else if(next == ']') {
					braceCount--;
				} 
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		if(braceCount != 0) {
			System.err.println("Error: Unmatched square braces.");
			System.err.println("Cannot continue.  Please review source.");
			// Cleaner option: Throw an exception to be caught?
			// Worth thinking about
			return "ERR";
		}
		return out;
	}
	public static String genWrapper(String className) {
		String head = "";
		head += "import java.util.Scanner;\n";
		head += "public class " + className.substring(0,className.indexOf('.')) + " {\n";
		head += "public static void main(String[] args) { \n";
		head += "byte[] mem = new byte[30000];\n";
		head += "Scanner iostream = new Scanner(System.in);\n";
		head += "int ptr = 0;\n";
		return head;
	}
	public static String genTail() {
		String tail = "";
		tail += "iostream.close();\n";
		tail += "}\n"; // close main method
		tail += "}\n"; // close class body
		return tail;
	}
	public static void writeJavaSource(String outName, String header, String body, String tail) {
		String program = header + body + tail;
		File outSource = new File(outName);
		try {
			PrintWriter printer = new PrintWriter(outSource);
			printer.println(program);
			printer.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	private static void compile(String fName) {
		try {
			Process shell = Runtime.getRuntime().exec("javac " + fName);
			shell.waitFor();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	public static void main(String[] args) {
		if(validateArgs(args) && sourceExists(args[0])) {
			String outName = getOutputFile(args);
			String source = stripFormat(readFile(args[0]));
			String javaBody = translate(source);
			if(!javaBody.equals("ERR")) {
				writeJavaSource(outName, genWrapper(outName),javaBody,genTail());
				compile(outName);
			}
		}
	}
}

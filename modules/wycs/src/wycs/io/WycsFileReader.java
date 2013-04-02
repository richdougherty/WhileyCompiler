package wycs.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import wyautl.core.Automata;
import wyautl.core.Automaton;
import wyautl.io.BinaryAutomataReader;
import wyautl.util.BigRational;
import wybs.io.BinaryInputStream;
import wybs.lang.NameID;
import wybs.lang.Path;
import wybs.util.Pair;
import wybs.util.Trie;
import wycs.core.*;

public class WycsFileReader {
	private static final char[] magic = {'W','Y','C','S','F','I','L','E'};

	private Path.Entry<WycsFile> entry;
	private BinaryInputStream input;
	private String[] stringPool;
	private Path.ID[] pathPool;
	private NameID[] namePool;	
	private Value[] constantPool;
	private SemanticType[] typePool;
	
	public WycsFileReader(Path.Entry<WycsFile> entry, InputStream input) {
		this.entry = entry;
		this.input = new BinaryInputStream(input);
	}
	
	public WycsFile read() throws IOException{
		for(int i=0;i!=8;++i) {
			char c = (char) input.read_u8();
			if(magic[i] != c) {
				throw new IllegalArgumentException("invalid magic number");
			}
		}
		
		int majorVersion = input.read_uv();
		int minorVersion = input.read_uv();
		
		int stringPoolSize = input.read_uv();
		int pathPoolSize = input.read_uv();
		int namePoolSize = input.read_uv();
		int typePoolSize = input.read_uv();
		int constantPoolSize = input.read_uv();
				
		int numBlocks = input.read_uv();
		
		readStringPool(stringPoolSize);
		readPathPool(pathPoolSize);
		readNamePool(namePoolSize);
		readTypePool(typePoolSize);	
		readConstantPool(constantPoolSize);	
		
		input.pad_u8();
						
		return readModule();
	}
	
	private void readStringPool(int size) throws IOException {		
		final String[] myStringPool = new String[size];
		
		for(int i=0;i!=size;++i) {
			int length = input.read_uv();
			try {
				byte[] data = new byte[length];
				input.read(data);
				String str = new String(data,0,length,"UTF-8");
				myStringPool[i] = str;
			} catch(UnsupportedEncodingException e) {
				throw new RuntimeException("UTF-8 Charset not supported?");
			}
		}
		stringPool = myStringPool;
	}
	
	private void readPathPool(int size) throws IOException {
		final Path.ID[] myPathPool = new Path.ID[size];
		myPathPool[0] = Trie.ROOT;
		
		for (int i = 1; i != size; ++i) {
			int parent = input.read_uv();
			int stringIndex = input.read_uv();
			Path.ID id;
			id = myPathPool[parent];
			id = id.append(stringPool[stringIndex]);
			myPathPool[i] = id;
		}
		pathPool = myPathPool;
	}

	private void readNamePool(int size) throws IOException {
		final NameID[] myNamePool = new NameID[size];
		
		for (int i = 0; i != size; ++i) {
			// int kind = input.read_uv();
			int pathIndex = input.read_uv();
			int nameIndex = input.read_uv();
			Path.ID id = pathPool[pathIndex];
			String name = stringPool[nameIndex];
			myNamePool[i] = new NameID(id, name);
		}
		
		namePool = myNamePool;
	}

	private void readConstantPool(int size) throws IOException {		
		final Value[] myConstantPool = new Value[size];
				
		for(int i=0;i!=size;++i) {
			int code = input.read_uv();
			Value constant;
			
			switch (code) {
				case WycsFileWriter.CONSTANT_False :
					constant = Value.Bool(false);
					break;
				case WycsFileWriter.CONSTANT_True :
					constant = Value.Bool(true);
					break;
				case WycsFileWriter.CONSTANT_Int : {
					int len = input.read_uv();
					byte[] bytes = new byte[len];
					input.read(bytes);
					BigInteger bi = new BigInteger(bytes);
					constant = Value.Integer(bi);
					break;
				}
				case WycsFileWriter.CONSTANT_Real : {
					int len = input.read_uv();
					byte[] bytes = new byte[len];
					input.read(bytes);
					BigInteger num = new BigInteger(bytes);
					len = input.read_uv();
					bytes = new byte[len];
					input.read(bytes);
					BigInteger den = new BigInteger(bytes);
					BigRational br = new BigRational(num, den);
					constant = Value.Rational(br);
					break;
				}
				case WycsFileWriter.CONSTANT_String : {
					int index = input.read_uv();
					constant = Value.String(stringPool[index]);
					break;
				}
				case WycsFileWriter.CONSTANT_Set : {
					int len = input.read_uv();
					ArrayList<Value> values = new ArrayList<Value>();
					for (int j = 0; j != len; ++j) {
						int index = input.read_uv();
						values.add(myConstantPool[index]);
					}
					constant = Value.Set(values);
					break;
				}
				case WycsFileWriter.CONSTANT_Tuple : {
					int len = input.read_uv();
					ArrayList<Value> values = new ArrayList<Value>();
					for (int j = 0; j != len; ++j) {
						int index = input.read_uv();
						values.add(myConstantPool[index]);
					}
					constant = Value.Tuple(values);
					break;
				}				
				default:
					throw new RuntimeException(
							"Unknown constant encountered: " + code);
			}			
			myConstantPool[i] = constant;
		}
		
		constantPool = myConstantPool;
	}

	private void readTypePool(int size) throws IOException {		
		final SemanticType[] myTypePool = new SemanticType[size];
		BinaryAutomataReader reader = new BinaryAutomataReader(input,Types.SCHEMA);
		Automaton global = reader.read();
		
		for(int i=0;i!=size;++i) {
			Automaton automaton = new Automaton();
			int root = automaton.addAll(global.getRoot(i), global);
			automaton.setRoot(0, root);
			SemanticType t = SemanticType.construct(automaton);
			myTypePool[i] = t;					
		}
		
		typePool = myTypePool;
	}
	
	private WycsFile readModule() throws IOException {		
		int kind = input.read_uv(); // block identifier
		int size = input.read_uv();
		input.pad_u8();
		
		int pathIdx = input.read_uv();
		int numBlocks = input.read_uv();
		
		input.pad_u8();
		
		List<WycsFile.Declaration> declarations = new ArrayList<WycsFile.Declaration>();
		for(int i=0;i!=numBlocks;++i) {			
			declarations.add(readModuleBlock());
		}
		
		return new WycsFile(pathPool[pathIdx],null,declarations);
	}
	
	private WycsFile.Declaration readModuleBlock() throws IOException {
		int kind = input.read_uv();
		int size = input.read_uv();
		input.pad_u8();
		
		switch(kind) {
			case WycsFileWriter.BLOCK_Macro:
				return readMacroBlock();
			case WycsFileWriter.BLOCK_Function:
				return readFunctionBlock();
			case WycsFileWriter.BLOCK_Assert:
				return readAssertBlock();						
			default:
				throw new RuntimeException("unknown module block encountered (" + kind + ")");
		}
	}
	
	private WycsFile.Declaration readMacroBlock() throws IOException {
		int nameIdx = input.read_uv();
		int fromIdx = input.read_uv();
		int nGenerics = input.read_uv();
		String[] generics = new String[nGenerics];
		for(int i=0;i!=nGenerics;++i) {
			int idx = input.read_uv();
			generics[i] = stringPool[idx];
		}
		int nBlocks = input.read_uv();
		Code<?> code = readCodeBlock();
		input.pad_u8();
		
		return new WycsFile.Macro(stringPool[nameIdx], generics,
				typePool[fromIdx], code);
	}
	
	private WycsFile.Declaration readFunctionBlock() throws IOException {
		int nameIdx = input.read_uv();
		int fromIdx = input.read_uv();
		int toIdx = input.read_uv();
		int nGenerics = input.read_uv();
		String[] generics = new String[nGenerics];
		for(int i=0;i!=nGenerics;++i) {
			int idx = input.read_uv();
			generics[i] = stringPool[idx];
		}
		int nBlocks = input.read_uv();
		Code<?> code = null;
		if(nBlocks > 0) {
			code = readCodeBlock();
		}
		input.pad_u8();
		return new WycsFile.Function(stringPool[nameIdx], generics,
				typePool[fromIdx], typePool[toIdx], code);
	}
	
	private WycsFile.Declaration readAssertBlock() throws IOException {
		int nameIdx = input.read_uv();
		int nBlocks = input.read_uv();
		Code<?> code = readCodeBlock();
		input.pad_u8();
		return new WycsFile.Assert(stringPool[nameIdx], code);
	}
	
	private Code readCodeBlock() throws IOException {
		int opcode = input.read_u8();
		int typeIdx = input.read_uv();
		SemanticType type = typePool[typeIdx];
		int nOperands = input.read_uv();
		Code[] operands = new Code[nOperands];
		for(int i=0;i!=nOperands;++i) {
			operands[i] = readCodeBlock();
		}
		Code.Op op = op(opcode);
		
		switch(op) {
		case VAR: {
			int varIdx = input.read_uv();
			return Code.Variable(type,operands,varIdx);
		}
		case CONST: {
			int constIdx = input.read_uv();
			if(operands.length != 0) {
				throw new RuntimeException("invalid constant bytecode encountered");
			}
			return Code.Constant(constantPool[constIdx]);
		}
		case NOT:
		case NEG:
		case LENGTH:
			if(operands.length != 1) {
				throw new RuntimeException("invalid unary bytecode encountered");
			}
			return Code.Unary(type,op,operands[0]);
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case REM:			
		case EQ:
		case NEQ:
		case LT:
		case LTEQ:
		case IN:
		case SUBSET:
		case SUBSETEQ:
			if(operands.length != 2) {
				throw new RuntimeException("invalid binary bytecode encountered");
			}
			return Code.Binary(type,op,operands[0],operands[1]);
		case AND:
		case OR:
		case TUPLE:
		case SET:
			return Code.Nary(type, op, operands);
		case LOAD: {
			if(operands.length != 1 || !(type instanceof SemanticType.Tuple)) {
				throw new RuntimeException("invalid load bytecode encountered");
			}
			int index = input.read_uv();
			return Code.Load((SemanticType.Tuple)type,operands[0],index);
		}
		case FORALL:
		case EXISTS: {
			if (operands.length != 1) {
				throw new RuntimeException(
						"invalid quantifier bytecode encountered");
			}
			int length = input.read_uv();
			Pair<SemanticType, Integer>[] types = new Pair[length];
			for (int i = 0; i != length; ++i) {
				int pTypeIdx = input.read_uv();
				int pVarIdx = input.read_uv();
				types[i] = new Pair<SemanticType, Integer>(typePool[pTypeIdx],
						pVarIdx);
			}
			return Code.Quantifier(type, op, operands[0], types);
		}
		}
		
		throw new RuntimeException("unknown opcode encountered: " + opcode);		
	}
	
	private Code.Op op(int opcode) {
		for (Code.Op op : Code.Op.values()) {
			if (op.offset == opcode) {
				return op;
			}
		}
		throw new RuntimeException("unknown opcode encountered: " + opcode);
	}
}
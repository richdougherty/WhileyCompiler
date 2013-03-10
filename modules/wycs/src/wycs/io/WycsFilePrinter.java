package wycs.io;

import java.io.*;
import java.util.*;

import static wybs.lang.SyntaxError.*;
import wybs.util.Pair;
import wycs.lang.*;
import wycs.lang.Expr.Quantifier;

public class WycsFilePrinter {
	public static final String INDENT = "  ";
	
	private PrintStream out;
	
	public WycsFilePrinter(PrintStream writer) {
		this.out = writer;
	}
		
	public WycsFilePrinter(OutputStream writer) {
		try {
			this.out = new PrintStream(writer, true, "UTF-8");
		} catch(UnsupportedEncodingException e) {
			this.out = new PrintStream(writer);
		}
	}
	
	public void write(WycsFile wf) {
		for(WycsFile.Declaration d : wf.declarations()) {
			write(wf, d);
			out.println();
		}
		out.flush();
	}	
	
	private void write(WycsFile wf, WycsFile.Declaration s) {
		if(s instanceof WycsFile.Function) {
			write((WycsFile.Function)s);
		} else if(s instanceof WycsFile.Assert) {
			write((WycsFile.Assert)s);
		} else if(s instanceof WycsFile.Import) {
			write((WycsFile.Import)s);
		} else {
			internalFailure("unknown statement encountered " + s,
					wf.filename(), s);
		}
	}
	
	public void write(WycsFile.Import s) {
		if (s.name == null) {
			out.print("import " + s.filter);
		} else {
			out.print("import " + s.name + " from " + s.filter);
		}
	}
	
	public void write(WycsFile.Function s) {
		if(s instanceof WycsFile.Define) {
			out.print("define ");
		} else {
			out.print("function ");
		}
		out.print(s.name);
		if(s.generics.size() > 0) {
			out.print("<");
			boolean firstTime=true;
			for(String g : s.generics) {
				if(!firstTime) {
					out.print(", ");
				}
				firstTime=false;
				out.print(g);
			}
			out.print(">");
		}
		boolean firstTime=true;
		out.print(s.from);
		if(!(s instanceof WycsFile.Define)) {
			out.print(" => " + s.to);
		}
		if(s.condition != null) {
			out.print(" where ");
			writeWithoutBraces(s.condition);
		}
		out.println();
	}
	
	public void write(WycsFile.Assert s) {
		out.print("assert ");
		if(s.message != null) {
			out.print("\"" + s.message + "\" ");
		}
		writeWithoutBraces(s.expr);
		out.println();
	}
	
	public void writeWithBraces(Expr e) {
		boolean needsBraces = needsBraces(e);
		if(needsBraces) {
			out.print("(");
			writeWithoutBraces(e);
			out.print(")");
		} else {
			writeWithoutBraces(e);
		}
	}
	
	public void writeWithoutBraces(Expr e) {
		if(e instanceof Expr.Unary) {
			write((Expr.Unary)e);
		} else if(e instanceof Expr.Nary) {
			write((Expr.Nary)e);
		} else if(e instanceof Expr.Quantifier) {
			write((Expr.Quantifier)e);
		} else if(e instanceof Expr.Binary) {
			write((Expr.Binary)e);
		} else {
			out.print(e);
		}
	}
	
	private void write(Expr.Unary e) {
		out.print(e.op);
		writeWithBraces(e.operand);					
	}
	
	private void write(Expr.Nary e) {
		switch(e.op) {
		case AND:
		case OR:
			String op = e.op == Expr.Nary.Op.AND ? "&&" : "||";
			boolean firstTime=true;
			for(Expr operand : e.operands) {
				if(!firstTime) {
					out.print(" " + op + " ");
				} else {
					firstTime = false;
				}			
				writeWithBraces(operand);				
			}
			return;
		}
		out.print(e.toString());
	}
	
	private void write(Expr.Binary e) {
		switch(e.op) {
		case IMPLIES:			
			writeWithBraces(e.leftOperand);
			out.print(" ==> ");			
			writeWithBraces(e.rightOperand);
			return;
		}
		out.print(e.toString());
	}
	
	private void write(Expr.Quantifier e) {
		if(e instanceof Expr.ForAll) {
			out.print("forall [ ");
		} else {
			out.print("some [ ");
		}
		
		boolean firstTime=true;
		for (Pair<TypePattern,Expr> p : e.variables) {			
			if (!firstTime) {
				out.print(", ");
			} else {
				firstTime = false;
			}
			out.print(p.first());
			if(p.second() != null) {
				out.print(" in " + p.second());
			}
		}		
		out.print(" : ");
		writeWithoutBraces(e.operand);
		out.print("]");
	}
	
	private static boolean needsBraces(Expr e) {
		 if(e instanceof Expr.Binary) {			
			 Expr.Binary be = (Expr.Binary) e;
			 switch(be.op) {
			 case IMPLIES:
				 return true;
			 }
		 } else if(e instanceof Expr.Nary) {
			 Expr.Nary ne = (Expr.Nary) e;
			 switch(ne.op) {
			 case AND:
			 case OR:
				 return true;
			 }
		 } 
		 return false;
	}
}
package wyone.core;

import java.util.*;
import wyone.util.*;

public class SpecFile {
	public final String filename;
	public final ArrayList<Decl> declarations;
	
	public SpecFile(String filename, Collection<Decl> declarations) {
		this.filename = filename;
		this.declarations = new ArrayList<Decl>(declarations);
	}
	
	public interface Decl extends SyntacticElement {}
	
	public static class TermDecl extends SyntacticElement.Impl implements Decl {
		public final Type.Term type;
		
		public TermDecl(Type.Term data, Attribute... attributes) {
			super(attributes);
			this.type = data;
		}		
	}
	
	public static class ClassDecl extends SyntacticElement.Impl implements Decl {
		public final String name;
		public final List<String> children;
		
		public ClassDecl(String n, Collection<String> children, Attribute... attributes) {
			super(attributes);
			this.name = n;
			this.children = new ArrayList<String>(children);
		}				
	}
	
	public static class TypeDecl extends SyntacticElement.Impl implements SyntacticElement {
		public final Type type;
		
		public TypeDecl(Type type, Attribute... attributes) {
			super(attributes);
			this.type = type;
		}		
	}
	
	public static class FunDecl extends SyntacticElement.Impl implements
			Decl {
		public final ArrayList<Code> codes;

		public FunDecl(Type.Fun type, List<Code> codes,
				Attribute... attributes) {
			super(attributes);
			this.codes = new ArrayList<Code>(codes);
		}
	}
}

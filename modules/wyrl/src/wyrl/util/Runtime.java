// Copyright (c) 2011, David J. Pearce (djp@ecs.vuw.ac.nz)
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
//    * Redistributions of source code must retain the above copyright
//      notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above copyright
//      notice, this list of conditions and the following disclaimer in the
//      documentation and/or other materials provided with the distribution.
//    * Neither the name of the <organization> nor the
//      names of its contributors may be used to endorse or promote products
//      derived from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
// WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
// DISCLAIMED. IN NO EVENT SHALL DAVID J. PEARCE BE LIABLE FOR ANY
// DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
// (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
// ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
// SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

package wyrl.util;

import java.io.IOException;

import wyautl.core.*;
import wyautl.io.BinaryAutomataReader;
import wybs.io.BinaryInputStream;
import wyrl.core.Type;
import wyrl.core.Types;
import wyrl.io.JavaIdentifierInputStream;

public class Runtime {
	
	
	/**
	 * Construct an <code>Automaton.List</code> representing the consecutive
	 * list of numbers between <code>start</code> and <code>end</code>
	 * (exclusive).
	 * 
	 * @param automaton
	 *            --- automaton into which to create this List.
	 * @param start
	 *            --- starting index
	 * @param end
	 *            --- final index
	 * @return
	 */
	public static Automaton.List rangeOf(Automaton automaton,
			Automaton.Int _start, Automaton.Int _end) {
		// FIXME: there is a bug here for big integer values.
		int start = _start.intValue();
		int end = _end.intValue();
		int[] children = new int[end - start];
		for (int i = 0; i < children.length; ++i, ++start) {
			children[i] = automaton.add(new Automaton.Int(start));
		}
		return new Automaton.List(children);
	}

	/**
	 * Construct a type from a string encoding of it. The string must be a
	 * binary encoding of the underlying automaton which is itself encoded as a
	 * Java string using the <code>JavaIdentifierOutputStream</code>.
	 * 
	 * @param b
	 * @return
	 */
	public static Type Type(String str) {
		try {
			JavaIdentifierInputStream jin = new JavaIdentifierInputStream(str);
			BinaryInputStream bin = new BinaryInputStream(jin);
			BinaryAutomataReader reader = new BinaryAutomataReader(bin, Types.SCHEMA);
			Automaton automaton = reader.read();
			reader.close();
			
			System.out.println("READ: " + Type.construct(automaton));
			
			return Type.construct(automaton);			
		} catch(IOException e) {
			throw new RuntimeException("runtime failure constructing type",e);
		}
	}
	
	/**
	 * Determine whether a given automaton is <i>accepted</i> by (i.e. contained
	 * in) an given type. For example, consider this very simple type:
	 * 
	 * <pre>
	 * term True
	 * term False
	 * define Bool as True | False
	 * </pre>
	 * 
	 * We can then ask the question as to whether or not the type
	 * <code>Bool</code> accepts the automaton which describes <code>True</code>
	 * . This function is used during rewriting to determine whether or not a
	 * given pattern leaf matches, and also for implementing the <code>is</code>
	 * operator
	 * 
	 * @param type
	 *            --- The type being to check for containment.
	 * @param automaton
	 *            --- The automaton being checked for inclusion.
	 * @return
	 */
	public static boolean accepts(Type type, Automaton automaton, int root, Schema schema) {
		
		// FIXME: this doesn't yet handle cyclic automata
		System.out.println("TESTING_a: " + type);
		
		Automaton type_automaton = type.automaton();		
		return accepts(type_automaton,type_automaton.getRoot(0),automaton,root,schema);
	}
	
	/**
	 * Determine whether a given automaton is <i>accepted</i> by (i.e. contained
	 * in) an given type. For example, consider this very simple type:
	 * 
	 * <pre>
	 * term True
	 * term False
	 * define Bool as True | False
	 * </pre>
	 * 
	 * We can then ask the question as to whether or not the type
	 * <code>Bool</code> accepts the automaton which describes <code>True</code>
	 * . This function is used during rewriting to determine whether or not a
	 * given pattern leaf matches, and also for implementing the <code>is</code>
	 * operator
	 * 
	 * @param type
	 *            --- The type being to check for containment.
	 * @param automaton
	 *            --- The automaton being checked for inclusion.
	 * @return
	 */
	public static boolean accepts(Type type, Automaton automaton,
			Automaton.State aState, Schema schema) {

		// FIXME: this doesn't yet handle cyclic automata
	
		Automaton type_automaton = type.automaton();
		return accepts(type_automaton, type_automaton.getRoot(0), automaton,
				aState, schema);
	}
	
	private static boolean accepts(Automaton type, int tIndex,
			Automaton automaton, int aIndex, Schema schema) {
		Automaton.Term tState = (Automaton.Term) type.get(tIndex);
		Automaton.State aState = automaton.get(aIndex);
		if (tState.kind == Types.K_Ref) {
			Automaton.Term tTerm = (Automaton.Term) tState;
			return accepts(type, tTerm.contents, automaton, aState, schema);
		} else {
			return false;
		}
	}
	
	private static boolean accepts(Automaton type, int tIndex,
			Automaton automaton, Automaton.State aState, Schema schema) {
		Automaton.Term tState = (Automaton.Term) type.get(tIndex);
		
		switch(tState.kind){
		case Types.K_Void:
			return false;
		case Types.K_Any:
			return true;
		case Types.K_Bool:
			return aState instanceof Automaton.Bool;
		case Types.K_Int:
			return aState instanceof Automaton.Int;
		case Types.K_Real:
			return aState instanceof Automaton.Real;
		case Types.K_String:
			return aState instanceof Automaton.Strung;
		case Types.K_Term:
			if(aState instanceof Automaton.Term) {
				Automaton.Term aTerm = (Automaton.Term) aState;
				return accepts(type,tState,automaton,aTerm,schema);				
			}
			return false;
		case Types.K_Nominal:
			return acceptsNominal(type,(Automaton.Term) tState,automaton,aState,schema);				
		case Types.K_Set:
			if(aState instanceof Automaton.Set) {
				Automaton.Set aSet = (Automaton.Set) aState;
				return accepts(type,tState,automaton,aSet,schema);				
			}
			return false;
		case Types.K_Bag:
			if(aState instanceof Automaton.Bag) {
				Automaton.Bag aBag = (Automaton.Bag) aState;
				return accepts(type,tState,automaton,aBag,schema);				
			}
			return false;			
		case Types.K_List:
			if(aState instanceof Automaton.List) {
				Automaton.List aList = (Automaton.List) aState;
				return accepts(type,tState,automaton,aList,schema);				
			}
			return false;
		case Types.K_Or:
			return acceptsOr(type,tState,automaton,aState,schema);							
		case Types.K_And:
			return acceptsAnd(type,tState,automaton,aState,schema);
		}
		
		// This should be dead-code since all possible cases are covered above.
		throw new IllegalArgumentException("unknowm type kind encountered ("
				+ tState.kind + ")");
	}
	
	private static boolean accepts(Automaton type, Automaton.Term tState,
			Automaton automaton, Automaton.Term aTerm, Schema schema) {
		Automaton.List list = (Automaton.List) type.get(tState.contents);
		String expectedName = ((Automaton.Strung) type.get(list.get(0))).value;
		String actualName = schema.get(aTerm.kind).name;
		if(!expectedName.equals(actualName)) {
			return false;
		} else if(list.size() == 1) {
			return aTerm.contents == Automaton.K_VOID;
		} else {
			return accepts(type,list.get(1),automaton,aTerm.contents,schema);
		}
	}
	
	private static boolean accepts(Automaton type, Automaton.Term tState,
			Automaton automaton, Automaton.Set aSet, Schema schema) {
		Automaton.List list = (Automaton.List) type.get(tState.contents);
		Automaton.Collection collection = (Automaton.Collection) type
				.get(list.get(1));
		Automaton.Term unbounded = (Automaton.Term) type.get(list.get(0));
		boolean isUnbounded = unbounded.kind != Types.K_Void;
		
		throw new RuntimeException("Need to implement Runtime.accepts(...);");
	}
	
	private static boolean accepts(Automaton type, Automaton.Term tState,
			Automaton automaton, Automaton.Bag aBag, Schema schema) {
		// TODO: implement this function!
		throw new RuntimeException("Need to implement Runtime.accepts(...);");
	}
	
	private static boolean accepts(Automaton type, Automaton.Term tState,
			Automaton automaton, Automaton.List aList, Schema schema) {
		// TODO: implement this function!
		throw new RuntimeException("Need to implement Runtime.accepts(...);");
	}
	
	private static boolean acceptsNominal(Automaton type,
			Automaton.Term tState, Automaton automaton, Automaton.State aState,
			Schema schema) {
		Automaton.List l = (Automaton.List) type.get(tState.contents);
		return accepts(type,l.get(1),automaton,aState,schema);
	}
	
	private static boolean acceptsAnd(Automaton type, Automaton.Term tState,
			Automaton automaton, Automaton.State aState, Schema schema) {
		Automaton.Set set = (Automaton.Set) type.get(tState.contents);
		for(int i=0;i!=set.size();++i) {
			int element = set.get(i);
			if(!accepts(type,element,automaton,aState,schema)) {
				return false;
			}
		}
		return true;
	}
	
	private static boolean acceptsOr(Automaton type, Automaton.Term tState,
			Automaton automaton, Automaton.State aState, Schema schema) {
		Automaton.Set set = (Automaton.Set) type.get(tState.contents);
		for(int i=0;i!=set.size();++i) {
			int element = set.get(i);
			if(accepts(type,element,automaton,aState,schema)) {
				return true;
			}
		}
		return false;
	}
}


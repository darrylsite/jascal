Jascal Scripting Engine
========================

Jascal is an implementation of JAVA JSR 223 for the Pascal Language.
The engine uses the Free-Pascal compiler for the JVM. It is tested on Windows and Linux.
It implements the Compilable interface. The support for the Invokable interface is planned in a future.
The Engine support passing ScriptEngine.ARGV to the context and bindings.

Unlinke the standard FPC for JVM, Jascal supports the write/writeln instructions, "ParamStr(i:integer) : String" function and has the "ParamCount : integer" variable. To achieve this result a slight transformation of the code is done before compilation. The following code is injected before compilation.

			program {$program_name$};

  			{ %VERSION=1.1 }
  			{$namespace com.darrylsite.jascal.fpc}
  
 			 {$mode objfpc}
  			 {$modeswitch unicodestrings}
 			 {$h+}
  
  			uses
   			 jdk15, scriptingstream;
  
  			type
  			  qprinttype = int64;
  
  			  var
  			        usedErrPutStream : JIWriter;
    			    programStream : OFRScriptingStreams;
			        commandLine : OFRCommandLine;
 			        ParamCount : integer;
 			        {$binding_vars$}

 			procedure initProgram(argv: Arr1JLString);
  			begin
   			 programStream :=  OFRScriptingStreams.create(); 
   			 commandLine :=  OFRCommandLine.create(argv);
    			ParamCount :=  commandLine.getParamCount();
  			end;
  
  			function ParamStr(i: integer): JLString;
  			begin
    			result := commandLine.getParamter(i);
 			 end;
  
  			procedure setOutPut(outPut: JIWriter);
 			 Begin
   			 programStream.setWriter(outPut);
  			end;
  
  			procedure setInPut(inPut: JIReader);
  			Begin
    			 programStream.setReader(inPut);
 			 end;
  
  			procedure setErrPut(errPut: JIWriter);
  			Begin
   			 usedErrPutStream := errPut;
 			 end;


Demo
-------

An online Pascal Compiler built on Jascal Scripting Engine is available at [http://jascal.darrylsite.com](http://jascal.darrylsite.com "Jascal script engine") 


Usage
-------

To use Jascal, add the JAR file in your project classPath. An example of a JAVA code using Jascal is given bellow. The full code is available in the unit test.

        ScriptEngineManager scriptManager = new ScriptEngineManager();
        ScriptEngine jascal = scriptManager.getEngineByName("Jascal");

        jascal.getContext().setWriter(new OutputStreamWriter(System.out));
        //String[] argv = new String[]{"1458796"};
        //jascal.put(ScriptEngine.ARGV, argv);
        jascal.put("nombre", "1478523699852"); //Binding, it initializes a global variable declared in the pascal code
        jascal.put("nombreTxt", null); //binding for getting the result value

        Compilable CompilableEngine = (Compilable) jascal;
        CompiledScript scriptCompile = CompilableEngine.compile(getScriptFile());

        scriptCompile.eval();
        

        //jascal.getContext().getWriter().flush();

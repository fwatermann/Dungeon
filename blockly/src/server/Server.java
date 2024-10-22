package server;

import antlr.blocklyConditionVisitor;
import antlr.main.blocklyLexer;
import antlr.main.blocklyParser;
import com.badlogic.gdx.Gdx;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import contrib.utils.components.Debugger;
import contrib.utils.components.skill.FireballSkill;
import contrib.utils.components.skill.Skill;
import core.Entity;
import core.Game;
import core.components.PositionComponent;
import core.components.VelocityComponent;
import core.utils.Point;
import core.utils.components.MissingComponentException;
import entities.VariableHUD;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nodes.StartNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * This class controls the communication between the blockly frontend and the dungeon. It has three
 * interfaces that can be used by the blockly frontend: /start /reset /clear The main interface is
 * "/start". This interface receives a string representing the code that should be executed in the
 * dungeon. The string can contain multiple lines or just one line. The "/reset" interface will stop
 * the current execution and will reset all values. The "/clear" interface will reset all values and
 * will typically be called after the blockly program run into an error or the execution finished
 * (when the whole program was executed and not just a step). The most important function of this
 * class is the function "processAction". This function will be executed for each action that needs
 * to be performed. If you add a block in the blockly-frontend this function is a good starting
 * point. You will probably want to add a new function to evaluate if the current action was
 * produced by your new block. You can easily check this with simple regex matching. If your regex
 * matched, perform the logic of your block. Place your function on the right place in the
 * processAction and think about side effects. For example: Does your block influence if other
 * action may be performed? Does your block needs its own scope? If yes, at least add it to the
 * active_scopes stack. Compare to while-loops/repeat-loops/if-statements and func definitions.
 * Don't forget to add a test to the TestServer class for your new block.
 */
public class Server {
  private static Entity hero;

  /**
   * This variable holds all active scopes in a stack. The value at the top of the stack is the
   * current scope. It can hold the following values: if, while, repeat, function.
   */
  public static final Stack<String> active_scopes = new Stack<>();

  /**
   * Hashmap storing all variables. This is public, so we can easily access it in the
   * blocklyConditionVisitor
   */
  public static final HashMap<String, Variable> variables = new HashMap<>();

  /** Hashmap storing all functions. */
  private static final HashMap<String, FuncStats> functions = new HashMap<>();

  /** Stack containing all active scopes. */
  public static final Stack<RepeatStats> active_repeats = new Stack<>();

  /** Stack containing all active while loops. */
  public static final Stack<WhileStats> active_whiles = new Stack<>();

  /** Stack containing all active ifs. */
  public static final Stack<IfStats> active_ifs = new Stack<>();

  /** Stack containing all active func defs. */
  public static final Stack<FuncStats> active_func_defs = new Stack<>();

  /**
   * This boolean will be set to true on error or if the user clicked the reset button in the
   * blockly frontend. The execution of the current program will stop if this variable is true.
   */
  public static boolean interruptExecution = false;

  /** This boolean will be set to true on error. */
  public static boolean errorOccurred = false;

  /** This variable cotnains the error message if an error occured during the execution. */
  public static String errorMsg = "";

  private static boolean clearHUD = false;
  private static final String[] reservedFunctions = {
    "oben",
    "unten",
    "links",
    "rechts",
    "feuerballOben",
    "feuerballUnten",
    "feuerballLinks",
    "feuerballRechts",
    "WandRechts",
    "WandLinks",
    "WandOben",
    "WandUnten",
    "naheWand"
  };
  private static final Stack<String> currently_repeating_scope = new Stack<>();

  /**
   * Object containing the variable HUD in the dungeon. This object is used to add new variables to
   * the HUD. It will add int variables and arrays to the HUD and update existing varaible values.
   */
  public static VariableHUD variableHUD = null;

  /**
   * Constructor of the server. Sets the hero.
   *
   * @param hero The hero entity. Used to control the movement of the hero.
   */
  public Server(Entity hero) {
    Server.hero = hero;
  }

  /**
   * Start the server and listen on the start, reset, clear interfaces. This server will be started
   * by the client.
   *
   * @throws IOException
   */
  public void start() throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 8080), 0);
    HttpContext startContext = server.createContext("/start");
    startContext.setHandler(Server::handleStartRequest);
    HttpContext resetContext = server.createContext("/reset");
    resetContext.setHandler(Server::handleResetRequest);
    HttpContext clearContext = server.createContext("/clear");
    clearContext.setHandler(Server::handleClearRequest);
    server.start();
  }

  /**
   * Handle the start request. It receives the program that should be executed in the dungeon from
   * the request body. It will process each action step by step. If the execution was interrupted by
   * an error or the reset button was clicked the execution will be stopped. In this case all
   * variables and other values will be cleared. If the program run into an error the response to
   * the blockly frontend will contain an error message.
   *
   * @param exchange
   * @throws IOException
   */
  private static void handleStartRequest(HttpExchange exchange) throws IOException {
    if (clearHUD) {
      clearHUDValues();
      clearHUD = false;
    }
    InputStream inStream = exchange.getRequestBody();
    String text = new String(inStream.readAllBytes(), StandardCharsets.UTF_8);

    String[] actions = text.split("\n");

    String errAction = null;
    for (String action : actions) {
      action = action.trim();
      processAction(action);
      if (interruptExecution) {
        errAction = action;
        break;
      }
    }
    // Build response for blockly frontend
    String response;
    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    if (interruptExecution) {
      System.out.println("Interruption performed");
      if (errorOccurred) {
        String msg = "Anweisung: " + errAction + "\n";
        msg += "Fehlermeldung: " + errorMsg;
        response = msg;
        exchange.sendResponseHeaders(400, response.getBytes().length);
      } else {
        response = "Execution interrupted";
        exchange.sendResponseHeaders(205, response.getBytes().length);
      }
      clearGlobalValues();
    } else {
      response = "OK";
      exchange.sendResponseHeaders(200, response.getBytes().length);
    }

    OutputStream os = exchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  /** Clear the variable and array HUD in the dungeon. */
  private static void clearHUDValues() {
    if (variableHUD == null) {
      return;
    }
    variableHUD.clearArrayVariables();
    variableHUD.clearVariables();
  }

  /**
   * Handles the reset request. This function will set the boolean interruptExecution. The execution
   * will be stopped.
   *
   * @param exchange Exchange object. The function will send a success response to the blockly
   *     frontend
   * @throws IOException
   */
  private static void handleResetRequest(HttpExchange exchange) throws IOException {
    // Reset values
    interruptExecution = true;

    Debugger.TELEPORT_TO_START();

    PositionComponent pc = getHeroPosition();
    String response = pc.position().x + "," + pc.position().y;

    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(200, response.getBytes().length);
    OutputStream os = exchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  /**
   * Handles the clear request. This function will clear all global variables that may have been
   * modified. It will empty all stacks, hashmaps and resets all global variables to their default
   * value.
   *
   * @param exchange Exchange object. The function will send a success response to the blockly
   *     frontend
   * @throws IOException
   */
  private static void handleClearRequest(HttpExchange exchange) throws IOException {
    clearGlobalValues();

    PositionComponent pc = getHeroPosition();
    String response = pc.position().x + "," + pc.position().y;

    exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
    exchange.sendResponseHeaders(200, response.getBytes().length);
    OutputStream os = exchange.getResponseBody();
    os.write(response.getBytes());
    os.close();
  }

  /**
   * Clear all global variables that may have been modified. Empty all stacks, hashmaps and reset
   * all global variables to their default value. Also clear the variable and array HUD in the
   * dungeon.
   */
  public static void clearGlobalValues() {
    // Reset values
    active_scopes.clear();
    currently_repeating_scope.clear();
    active_ifs.clear();
    active_whiles.clear();
    active_repeats.clear();
    active_func_defs.clear();
    variables.clear();
    functions.clear();
    interruptExecution = false;
    errorOccurred = false;
    errorMsg = "";
    // Set clear HUD to true so the HUD will be cleared next time the start route will be used.
    clearHUD = true;
    System.out.println("Values cleared");
    printScopes();
  }

  /**
   * Set the error flag and stop the execution. Also set the error message to the provided message.
   *
   * @param errMsg Error message that will be sent to the blockly frontend.
   */
  private static void setError(String errMsg) {
    interruptExecution = true;
    errorOccurred = true;
    errorMsg = errMsg;
  }

  /**
   * This function will be called when a scope is repeating itself. This can be the case for while
   * or repeat loops. It will process the actions of the loop until the condition of the loop is
   * false.
   */
  private static void repeatHook() {
    if (!currently_repeating_scope.isEmpty()) {
      String currentLoop = currently_repeating_scope.peek();
      switch (currentLoop) {
        case "while" -> {
          WhileStats currentWhile = active_whiles.peek();
          while (currentWhile.isRepeating && !interruptExecution) {
            System.out.print("Repeating while loop");
            System.out.println(currentWhile);
            for (String whileAction : currentWhile.whileBody) {
              System.out.println("[In while loop]");
              processAction(whileAction);
              if (interruptExecution) {
                break;
              }
            }
          }
        }
        case "repeat" -> {
          RepeatStats currentRepeat = active_repeats.peek();
          while (currentRepeat.isRepeating && !interruptExecution) {
            System.out.print("Repeating repeat loop");
            System.out.println(currentRepeat);
            for (String repeatAction : currentRepeat.repeatBody) {
              System.out.println("[In repeat loop]");
              processAction(repeatAction);
              if (interruptExecution) {
                break;
              }
            }
          }
        }
        default -> System.out.println("Unknown repeating scope");
      }
    }
  }

  /**
   * This function will process the given action. The action must be given without any whitespaces
   * at the end or beginning of the string. This function takes care of the following: 1: Scopes ->
   * Evaluate if the current scope must be closed or a new scopes must be opened. Possible scopes
   * are: if, while, repeat, function. 2: Variables -> Evaluate if a new variable must be created or
   * an existing variable must be adjusted. This includes int variables or int array variables. 3:
   * Execute actions in the dungeon -> Perform the desired actions in the dungeon. This can be
   * moving into a specific direction or throwing a fireball.
   *
   * @param action
   */
  public static void processAction(String action) {
    System.out.print("Processing action: ");
    System.out.println(action);
    // Make sure we close the right scope
    addActionToWhileBody(action);
    addActionToRepeatBody(action);
    addActionToFunc(action);
    if (action.equals("}") && !active_scopes.isEmpty()) {
      System.out.println("End of if, while or repeat detected");
      String current_scope = active_scopes.peek();
      switch (current_scope) {
        case "if" -> {
          System.out.println("eval if cond");
          ifEvaluation(action);
          System.out.print("Scopes after eval: ");
          System.out.println(active_scopes);
          return;
        }
        case "while" -> {
          System.out.println("eval while loop");
          boolean whileIsRepeating = closeWhile(action);
          System.out.print("Scopes after eval: ");
          System.out.println(active_scopes);
          if (whileIsRepeating) {
            repeatHook();
          }
          return;
        }
        case "repeat" -> {
          System.out.println("eval repeat loop");
          boolean repeatIsRepeating = closeRepeat(action);
          System.out.print("Scopes after eval: ");
          System.out.println(active_scopes);
          if (repeatIsRepeating) {
            repeatHook();
          }
          return;
        }
        case "function" -> {
          System.out.println("eval function");
          closeFunc(action);
          System.out.print("Scopes after eval: ");
          System.out.println(active_scopes);
          return;
        }
      }
    }

    ifEvaluation(action);
    whileEvaluation(action);
    repeatEvaluation(action);
    funcEvaluation(action);

    // Do not perform any actions when:
    // 1: Currently in func definition
    // 2: The condition of a while loop is false
    // 3: The condition of an if-statement is false
    if (!evalActionsExecute()) {
      return;
    }

    // Variable and func call evaluation are not allowed to be actually performed if while or if
    // condition is false or
    // a function is currently defined
    variableEvaluation(action);
    funcCallEvaluation(action);

    printScopes();

    performAction(action);
  }

  /**
   * Check if actions should be executed. Do not perform any action if one of the following
   * conditions is true: 1. At least one if-condition of the currently active if-scopes is false 2.
   * At least one while-condition of the currently active while-scopes is false 3: A function is
   * currently being defined
   *
   * @return Returns true if actions may be performed. Returns false if no action may be performed.
   */
  private static boolean evalActionsExecute() {
    return evalIfConditions() && evalWhileConditions() && active_func_defs.isEmpty();
  }

  /**
   * Eval if any if-condition is currently false.
   *
   * @return Returns true if all conditions are true or no if is currently active. Otherwise,
   *     returns false.
   */
  private static boolean evalIfConditions() {
    if (active_ifs.isEmpty()) {
      return true;
    }
    for (IfStats ifStatement : active_ifs) {
      if (!ifStatement.executeAction()) {
        return false;
      }
    }
    return true;
  }

  /**
   * Eval if any while-condition is currently false.
   *
   * @return Returns true if all conditions are true or no while-loop is currently active.
   *     Otherwise, returns false.
   */
  private static boolean evalWhileConditions() {
    if (active_whiles.isEmpty()) {
      return true;
    }
    for (WhileStats whileStatement : active_whiles) {
      if (!whileStatement.conditionResult) {
        return false;
      }
    }
    return true;
  }

  /** Prints all current scopes, repeating scopes, variables and functions. */
  private static void printScopes() {
    System.out.print("Current scopes: ");
    System.out.println(active_scopes);
    System.out.print("Currently repeating scopes: ");
    System.out.println(currently_repeating_scope);
    System.out.print("Current variables: ");
    System.out.println(variables);
    System.out.print("Available functions: ");
    System.out.println(functions);
  }

  /**
   * Retrieve an array variable from the variables hashmap. It will either throw a
   * NoSuchElementException when the variable name could not be found or an IllegalAccessException
   * if the variable does not have the type "array".
   *
   * @param varName Name of the variable that should be retrieved
   * @return Returns the variable from the hashmap
   * @throws IllegalAccessException
   */
  private static Variable getArrayVariable(String varName) throws IllegalAccessException {
    Variable array_var = variables.get(varName);
    // Throw exception if nothing found
    if (array_var == null) {
      throw new NoSuchElementException(String.format("Variable not found %s", varName));
    }
    if (!array_var.type.equals("array")) {
      throw new IllegalAccessException(
          String.format(
              "Expected array variable. Got %s for variable %s", array_var.type, varName));
    }
    return array_var;
  }

  /**
   * Get the actual value from an expression. This might be the value itself or the expression
   * contains a variable. Return the value of the variable or the value of an array expression if
   * given. Return the value of the variable in this case. Throws a NumberFormatException when a
   * value that is not a variable can not be parsed as an integer. Throws an IllegalAccessException
   * when an integer variable is expected but an array variable as given. Throws the same exception
   * if it's the other way around.
   *
   * @param value Value of the expression
   * @return Returns the value as an integer
   */
  public static int getActualValueFromExpression(String value) throws IllegalAccessException {
    // Process array access
    Pattern pattern = Pattern.compile("(\\w+)\\[(\\d+)]");
    Matcher matcher = pattern.matcher(value);
    if (matcher.find()) {
      Variable array_var = getArrayVariable(matcher.group(1));
      int index = Integer.parseInt(matcher.group(2));
      return array_var.arrayVal[index];
    }
    // We might have to return the array length
    Pattern patternArraySize = Pattern.compile("(\\w+)\\.length");
    Matcher matcherArraySize = patternArraySize.matcher(value);
    if (matcherArraySize.find()) {
      Variable array_var = getArrayVariable(matcherArraySize.group(1));
      return array_var.arrayVal.length;
    }
    // Process usual values
    if (variables.get(value) == null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        throw new NumberFormatException(value + " is not a number or variable");
      }
    }
    // Process int variable access
    Variable var = variables.get(value);
    if (var.type.equals("base")) {
      return var.intVal;
    }
    throw new IllegalAccessException(
        String.format("Expected base variable. Got %s for variable %s", var.type, value));
  }

  /**
   * Execute an expression with the given values and operator.
   *
   * @param leftValue Left value of the expression
   * @param rightValue Right value of the expression
   * @param op Operator of the expression
   * @return Returns the result of the expression
   */
  private static int executeExpression(int leftValue, int rightValue, String op) {
    try {
      return switch (op) {
        case "+" -> leftValue + rightValue;
        case "-" -> leftValue - rightValue;
        case "*" -> leftValue * rightValue;
        case "/" -> leftValue / rightValue;
        default -> 0;
      };
    } catch (ArithmeticException e) {
      throw new ArithmeticException("Division by zero is not allowed.");
    }
  }

  /**
   * Evaluate if a function was called. Ignore the reserved function names. If the function pattern
   * matches but the function can not be found in the functions hashmap set the error flag an stop
   * the execution.
   *
   * @param action The current action that should be evaluated.
   */
  private static void funcCallEvaluation(String action) {
    Pattern pattern = Pattern.compile("(\\w+)\\(\\)");
    Matcher matcher = pattern.matcher(action);
    if (matcher.find()) {
      String funcName = matcher.group(1);
      if (Arrays.asList(reservedFunctions).contains(funcName)) {
        return;
      }
      FuncStats calledFunc = functions.get(funcName);
      if (calledFunc == null) {
        setError("Function " + funcName + " is not defined");
        System.out.println("Could not find function " + funcName);
        return;
      }
      System.out.println("Executing function" + funcName);
      for (String funcAction : calledFunc.funcBody) {
        System.out.println("[In function " + funcName + "]");
        processAction(funcAction);
      }
    }
  }

  /**
   * Check if the scope of a function ends with current action. The function definition ends if the
   * current action is "}" and the active scope is "function".
   *
   * @param action
   */
  private static void closeFunc(String action) {
    if (action.equals("}") && active_scopes.peek().equals("function")) {
      FuncStats finishedFunc = active_func_defs.pop();
      active_scopes.pop();
      functions.put(finishedFunc.name, finishedFunc);
    }
  }

  /**
   * Add the current action to all active function definitions.
   *
   * @param action Current action that will be added to the function body of all active func defs.
   */
  private static void addActionToFunc(String action) {
    if (!active_func_defs.isEmpty()) {
      for (FuncStats func : active_func_defs) {
        func.funcBody.add(action);
      }
    }
  }

  /**
   * Evaluate if a func definition was started with the current action. In this case push a new
   * function scope to the active scopes and add a new FuncStats object to the active_func_defs.
   *
   * @param action Current action that needs to be evaluated.
   */
  private static void funcEvaluation(String action) {
    Pattern pattern = Pattern.compile("public void (\\w+)\\(\\)");
    Matcher matcher = pattern.matcher(action);
    // If pattern matches we have a new func definition
    if (matcher.find()) {
      active_func_defs.push(new FuncStats(matcher.group(1)));
      active_scopes.push("function");
    }
  }

  /**
   * Add a new base variable to the variables hashmap. Also display it in the dungeon variable HUD
   * if not null.
   *
   * @param name Name of the variable.
   * @param value Integer value of the variable.
   */
  private static void addBaseVar(String name, int value) {
    variables.put(name, new Variable(value));
    if (variableHUD != null) {
      variableHUD.addVariable(name, value);
    }
  }

  /**
   * Update the array HUD if not null.
   *
   * @param name Name of the array variable.
   * @param value Integer array value of the array variable
   */
  private static void updateArrayHUD(String name, int[] value) {
    if (variableHUD != null) {
      variableHUD.addArrayVariable(name, value);
    }
  }

  /**
   * Evaluate if a new variable was introduces or an existing variable was modified. This function
   * checks for usual int variables and int array variables.
   *
   * @param action Current action that needs to be evaluated.
   */
  private static void variableEvaluation(String action) {
    // Check array creation
    Pattern patternArray = Pattern.compile("int\\[] (\\w+) = new int\\[(\\d+)]");
    Matcher matcherArray = patternArray.matcher(action);
    if (matcherArray.find()) {
      int array_size = Integer.parseInt(matcherArray.group(2));
      variables.put(matcherArray.group(1), new Variable(new int[array_size]));
      updateArrayHUD(matcherArray.group(1), new int[array_size]);
      return;
    }
    // Check array assign
    if (checkArrayAssign(action)) return;
    // Check assign to normal variable with expression
    if (checkAssign(action)) return;

    // Simple assign
    Pattern pattern = Pattern.compile("int (\\w+) = (\\d+);");
    Matcher matcher = pattern.matcher(action);
    // If pattern matches we have a new variable
    if (matcher.find()) {
      addBaseVar(matcher.group(1), Integer.parseInt(matcher.group(2)));
    }
  }

  /**
   * Check if the current action is an assignment to an int variable. Sets the error flag in case of
   * an error in the blockly program. Updates the variable HUD.
   *
   * @param action Current action
   * @return Returns true if the current action matched the regex for variable assignments. Returns
   *     false if the regex did not match.
   */
  private static boolean checkAssign(String action) {
    // Check expression with operator
    Pattern pattern =
        Pattern.compile(
            "int (\\w+) = (\\w+(\\[\\d+])?(\\.length)?) (\\+|-|\\*|/) (\\w+(\\[\\d+])?(\\.length)?)");
    Matcher matcher = pattern.matcher(action);
    if (matcher.find()) {
      String leftVal = matcher.group(2);
      String rightVal = matcher.group(6);
      String varName = matcher.group(1);
      String op = matcher.group(5);
      try {
        int value = executeOperatorExpression(leftVal, rightVal, op);
        addBaseVar(varName, value);
        return true;
      } catch (IllegalAccessException
          | NoSuchElementException
          | IndexOutOfBoundsException
          | NumberFormatException
          | ArithmeticException e) {
        System.out.println(e.getMessage());
        setError(e.getMessage());
        return true;
      }
    }

    // Check single right value
    Pattern patternRightValue = Pattern.compile("int (\\w+) = (\\w+(\\[\\d+])?(\\.length)?)");
    Matcher matcherRightValue = patternRightValue.matcher(action);
    if (matcherRightValue.find()) {
      String varNameRightValue = matcherRightValue.group(1);
      String rightValue = matcherRightValue.group(2);
      try {
        int value = getActualValueFromExpression(rightValue);
        addBaseVar(varNameRightValue, value);
        return true;
      } catch (IllegalAccessException
          | NoSuchElementException
          | IndexOutOfBoundsException
          | NumberFormatException e) {
        System.out.println(e.getMessage());
        setError(e.getMessage());
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the current action is an assignment to an int array variable. Sets the error flag in
   * case of an error in the blockly program. Updates the array HUD.
   *
   * @param action Current action.
   * @return Returns true if the pattern von array assign matched. Otherwise, returns false.
   */
  private static boolean checkArrayAssign(String action) {
    Pattern pattern =
        Pattern.compile(
            "((\\w+)\\[(\\d+)]) = (\\w+(\\[\\d+])?(\\.length)?) (\\+|-|\\*|/) (\\w+(\\[\\d+])?(\\.length)?)");
    Matcher matcher = pattern.matcher(action);
    if (matcher.find()) {
      int index = Integer.parseInt(matcher.group(3));
      String leftVal = matcher.group(4);
      String rightVal = matcher.group(8);
      String varName = matcher.group(2);
      String op = matcher.group(7);
      try {
        int value = executeOperatorExpression(leftVal, rightVal, op);
        Variable arrayVar = getArrayVariable(varName);
        arrayVar.arrayVal[index] = value;
        updateArrayHUD(varName, arrayVar.arrayVal);
        return true;
      } catch (IllegalAccessException
          | NoSuchElementException
          | IndexOutOfBoundsException
          | NumberFormatException
          | ArithmeticException e) {
        System.out.println(e.getMessage());
        setError(e.getMessage());
        return true;
      }
    }

    // Check single right value
    Pattern patternRightValue =
        Pattern.compile("((\\w+)\\[(\\d+)]) = (\\w+(\\[\\d+])?(\\.length)?)");
    Matcher matcherRightValue = patternRightValue.matcher(action);
    if (matcherRightValue.find()) {
      String varNameRightValue = matcherRightValue.group(2);
      int indexRightValue = Integer.parseInt(matcherRightValue.group(3));
      String rightValue = matcherRightValue.group(4);
      try {
        int value = getActualValueFromExpression(rightValue);
        Variable arrayVar = getArrayVariable(varNameRightValue);
        arrayVar.arrayVal[indexRightValue] = value;
        updateArrayHUD(varNameRightValue, arrayVar.arrayVal);
        return true;
      } catch (IllegalAccessException
          | NoSuchElementException
          | IndexOutOfBoundsException
          | NumberFormatException e) {
        System.out.println(e.getMessage());
        setError(e.getMessage());
        return true;
      }
    }
    // Regex did not match. Return false in this case
    return false;
  }

  /**
   * Execute an operator expression. Retrieves the actual value from the given left and right value.
   *
   * @param leftVal Left value of the expression
   * @param rightVal Right value of the expression
   * @param op Operator of the expression
   * @return Returns the result of the expression as an integer
   * @throws IllegalAccessException
   */
  private static int executeOperatorExpression(String leftVal, String rightVal, String op)
      throws IllegalAccessException {
    // Get left and right value
    int leftValue = getActualValueFromExpression(leftVal);
    int rightValue = getActualValueFromExpression(rightVal);

    return executeExpression(leftValue, rightValue, op);
  }

  /**
   * Evaluate if the currently active while-loop must be closed. A while-loop will be closed if the
   * current action is "}", the current scope is "while" and the condition of the current while loop
   * evaluates to false. If the current while condition evaluates to true, set the isRepeating flag
   * for the current while.
   *
   * @param action Current action that needs to be evaluated
   * @return Returns true if the current while loop is repeating. Returns false if the current while
   *     loop was closed.
   */
  private static boolean closeWhile(String action) {
    Pattern pattern = Pattern.compile("solange \\((.*)\\)");
    // Check if loop must be ended
    if (action.equals("}") && active_scopes.peek().equals("while")) {
      WhileStats currentWhile = active_whiles.peek();
      String condition = currentWhile.condition;
      boolean conditionResult = evalComplexCondition(condition, pattern);
      if (conditionResult && evalActionsExecute()) {
        if (!currentWhile.isRepeating) {
          currentWhile.isRepeating = true;
          currently_repeating_scope.push("while");
          return true;
        }
      } else {
        System.out.println("Ending while loop");
        if (currentWhile.isRepeating) {
          currentWhile.isRepeating = false;
          currently_repeating_scope.pop();
        }
        active_scopes.pop();
        active_whiles.pop();
        return false;
      }
    }
    return false;
  }

  /**
   * Adds the current action to all active whiles.
   *
   * @param action Current action
   */
  private static void addActionToWhileBody(String action) {
    if (!active_whiles.isEmpty()) {
      if (active_whiles.peek().isRepeating) {
        return;
      }
      for (WhileStats whileLoop : active_whiles) {
        if (!whileLoop.isRepeating) {
          whileLoop.whileBody.add(action);
        }
      }
    }
  }

  /**
   * Evaluate if the current action is the beginning of a while loop. Push a new while loop onto the
   * active scopes stack and the active whiles stack.
   *
   * @param action Current action
   */
  private static void whileEvaluation(String action) {
    Pattern pattern = Pattern.compile("solange \\((.*)\\)");

    if (action.contains("solange")) {
      boolean currentConditionResult = evalComplexCondition(action, pattern);
      active_scopes.push("while");
      active_whiles.push(new WhileStats(action, currentConditionResult));
    }
  }

  /**
   * Evaluate if the current repeat loop must be closed. The repeat loop will be closed if the
   * current action is "}", the current scope is "repeat" and the repeat condition evaluates to
   * false. The repeat condition evaluates to false if the counter is equal or bigger than the
   * target counter. If the repeat condition evaluates to true, set the isRepeating flag.
   *
   * @param action Current action
   * @return Returns true if the current repeat loop is repeating. Returns false if the repeat loop
   *     was closed.
   */
  private static boolean closeRepeat(String action) {
    // Check if repeat loop must end
    if (action.equals("}") && active_scopes.peek().equals("repeat")) {
      RepeatStats currentRepeat = active_repeats.peek();
      // Check if repeat loops needs to be repeated
      if (!currentRepeat.evalRepeatComplete() && evalActionsExecute()) {
        if (!currentRepeat.isRepeating) {
          currentRepeat.isRepeating = true;
          currently_repeating_scope.push("repeat");
          currentRepeat.increaseCounter();
          return true;
        }
        currentRepeat.increaseCounter();
      } else {
        // Stop repeat loop if loop is finished or no actions may be performed
        if (currentRepeat.isRepeating) {
          currentRepeat.isRepeating = false;
          currently_repeating_scope.pop();
        }
        active_scopes.pop();
        active_repeats.pop();
        return false;
      }
    }
    return false;
  }

  /**
   * Adds the current action to all active repeat loops.
   *
   * @param action Current action
   */
  private static void addActionToRepeatBody(String action) {
    if (!active_repeats.isEmpty()) {
      if (active_repeats.peek().isRepeating) {
        return;
      }
      // Add current action to bodies of all active repeats that are not repeating
      for (RepeatStats repeatLoop : active_repeats) {
        if (!repeatLoop.isRepeating) {
          repeatLoop.repeatBody.add(action);
        }
      }
    }
  }

  /**
   * Evaluate if the current action is the beginning of a repeat loop. Push a new repeat scopes on
   * the active scopes stack and the active repeats stack.
   *
   * @param action Current action
   */
  private static void repeatEvaluation(String action) {
    Pattern pattern = Pattern.compile("wiederhole (\\w+) Mal");
    Matcher matcher = pattern.matcher(action);
    if (matcher.find()) {
      String repeatString = matcher.group(1);
      try {
        int value = getActualValueFromExpression(repeatString);
        active_scopes.push("repeat");
        active_repeats.push(new RepeatStats(value));
      } catch (IllegalAccessException
          | NoSuchElementException
          | IndexOutOfBoundsException
          | NumberFormatException e) {
        System.out.println(e.getMessage());
      }
    }
  }

  /**
   * Evaluate if the current action is the beginning or the end of an if-scope. Close the current
   * if-scope if the current action is "}" and the active scope is "if". If the current action is
   * the beginning of an if-scope push a new if-scope onto the active scopes stack and the
   * active_ifs stack. If the current action starts the else part of the if-statement set the
   * else-flag to the opposite of the if-flag and set the if-flag to false.
   *
   * @param action Current action
   */
  private static void ifEvaluation(String action) {
    if (action.equals("}") && !active_scopes.isEmpty() && active_scopes.peek().equals("if")) {
      active_ifs.pop();
      active_scopes.pop();
      return;
    }
    if (action.contains("falls")) {
      active_scopes.push("if");
      Pattern pattern = Pattern.compile("falls \\((.*)\\)");
      active_ifs.push(new IfStats(evalComplexCondition(action, pattern)));
    }

    if (action.contains("sonst")) {
      IfStats currentIf = active_ifs.peek();
      currentIf.else_flag = !currentIf.if_flag;
      currentIf.if_flag = false;
    }
  }

  /**
   * Evaluate a given condition. This function will use the blocklyConditionVisitor to evaluate the
   * given condition. The visitor will eventually throw a NoSuchElementException when a variable
   * could not be found. This function will set the error flag if an error occurred while parsing
   * the condition.
   *
   * @param action Current action
   * @param pattern Regex pattern that can be used to extract the condition from the action.
   * @return Returns the result of the condition or false on error.
   */
  public static boolean evalComplexCondition(String action, Pattern pattern) {
    if (!active_func_defs.isEmpty()) {
      return false;
    }
    Matcher matcher = pattern.matcher(action);
    if (matcher.find()) {
      blocklyLexer lexer = new blocklyLexer(CharStreams.fromString(matcher.group(1)));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      blocklyParser parser = new blocklyParser(tokens);

      ParseTree tree = parser.start();
      blocklyConditionVisitor eval = new blocklyConditionVisitor();
      try {
        StartNode ast = (StartNode) eval.visit(tree);
        boolean result = ast.getBoolValue();
        System.out.println("Result of current condition: " + result);
        return result;
      } catch (NoSuchElementException e) {
        System.out.println(e.getMessage());
        setError(e.getMessage());
        return false;
      }
    }
    System.out.println("Detected condition that is not valid: " + action);
    setError("Detected condition that is not valid: " + action);
    return false;
  }

  /**
   * Perform an actual action in the dungeon with the hero. This can either be movement or a
   * fireball in a specific direction.
   *
   * @param action Current action
   */
  private static void performAction(String action) {
    switch (action) {
      case "oben();" -> up();
      case "unten();" -> down();
      case "links();" -> left();
      case "rechts();" -> right();
        // case "interagieren();" -> interact();
      case "feuerballOben();" -> fireballUp();
      case "feuerballUnten();" -> fireballDown();
      case "feuerballLinks();" -> fireballLeft();
      case "feuerballRechts();" -> fireballRight();
    }
  }

  /** Move the hero up. */
  private static void up() {
    VelocityComponent vc =
        hero.fetch(VelocityComponent.class)
            .orElseThrow(() -> MissingComponentException.build(hero, VelocityComponent.class));
    vc.currentYVelocity(1 * vc.yVelocity());

    waitDelta();
  }

  /** Move the hero down. */
  private static void down() {
    VelocityComponent vc =
        hero.fetch(VelocityComponent.class)
            .orElseThrow(() -> MissingComponentException.build(hero, VelocityComponent.class));
    vc.currentYVelocity(-1 * vc.yVelocity());

    waitDelta();
  }

  /** Move the hero to the left. */
  private static void left() {
    VelocityComponent vc =
        hero.fetch(VelocityComponent.class)
            .orElseThrow(() -> MissingComponentException.build(hero, VelocityComponent.class));
    vc.currentXVelocity(-1 * vc.xVelocity());

    waitDelta();
  }

  /** Move the hero to the right. */
  private static void right() {
    VelocityComponent vc =
        hero.fetch(VelocityComponent.class)
            .orElseThrow(() -> MissingComponentException.build(hero, VelocityComponent.class));
    vc.currentXVelocity(1 * vc.xVelocity());

    waitDelta();
  }

  private static void waitDelta() {
    long timeout = (long) (Gdx.graphics.getDeltaTime() * 1000);
    try {
      TimeUnit.MILLISECONDS.sleep(timeout - 1);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Check if the hero is near a wall.
   *
   * @return Returns true if the hero is near to a wall. Otherwise, returns false.
   */
  public static boolean isNearWall() {
    boolean isNearWall;

    isNearWall = isNearWallUp() || isNearWallDown() || isNearWallLeft() || isNearWallRight();
    return isNearWall;
  }

  /**
   * Check if a wall is above the hero.
   *
   * @return Returns true if a wall is above the hero. Otherwise, returns false.
   */
  public static boolean isNearWallUp() {
    boolean isNearWallUp;

    PositionComponent pc = getHeroPosition();
    Point newPositionUp = new Point(pc.position().x, pc.position().y + 0.3f);

    isNearWallUp = !Game.tileAT(newPositionUp).isAccessible();
    return isNearWallUp;
  }

  /**
   * Check if a wall is below the hero.
   *
   * @return Returns true if a wall is below the hero. Otherwise, returns false.
   */
  public static boolean isNearWallDown() {
    boolean isNearWallDown;

    PositionComponent pc = getHeroPosition();
    Point newPositionDown = new Point(pc.position().x, pc.position().y - 0.3f);

    isNearWallDown = !Game.tileAT(newPositionDown).isAccessible();
    return isNearWallDown;
  }

  /**
   * Check if a wall is to the left of the hero.
   *
   * @return Returns true if a wall is to the left the hero. Otherwise, returns false.
   */
  public static boolean isNearWallLeft() {
    boolean isNearWallLeft;

    PositionComponent pc = getHeroPosition();
    Point newPositionLeft = new Point(pc.position().x - 0.3f, pc.position().y);

    isNearWallLeft = !Game.tileAT(newPositionLeft).isAccessible();
    return isNearWallLeft;
  }

  /**
   * Check if a wall is to the right of the hero.
   *
   * @return Returns true if a wall is to the right the hero. Otherwise, returns false.
   */
  public static boolean isNearWallRight() {
    boolean isNearWallRight;

    PositionComponent pc = getHeroPosition();
    Point newPositionRight = new Point(pc.position().x + 0.3f, pc.position().y);

    isNearWallRight = !Game.tileAT(newPositionRight).isAccessible();
    return isNearWallRight;
  }

  /**
   * Get the current position of the hero in the dungeon.
   *
   * @return Returns a position component which contains the x and y coordinates of the hero in the
   *     dungeon.
   */
  public static PositionComponent getHeroPosition() {
    return hero.fetch(PositionComponent.class)
        .orElseThrow(() -> MissingComponentException.build(hero, PositionComponent.class));
  }

  /** Throw a fireball upwards. */
  public static void fireballUp() {
    Skill fireball =
        new Skill(
            new FireballSkill(
                new Supplier<Point>() {
                  @Override
                  public Point get() {
                    Point heroPoint =
                        new Point(getHeroPosition().position().x, getHeroPosition().position().y);
                    heroPoint.y += 1;
                    return heroPoint;
                  }
                }),
            1);
    fireball.execute(hero);
    waitDelta();
  }

  /** Throw a fireball downwards. */
  public static void fireballDown() {
    Skill fireball =
        new Skill(
            new FireballSkill(
                new Supplier<Point>() {
                  @Override
                  public Point get() {
                    Point heroPoint =
                        new Point(getHeroPosition().position().x, getHeroPosition().position().y);
                    heroPoint.y -= 1;
                    return heroPoint;
                  }
                }),
            1);
    fireball.execute(hero);
    waitDelta();
  }

  /** Throw a fireball to the left. */
  public static void fireballLeft() {
    Skill fireball =
        new Skill(
            new FireballSkill(
                new Supplier<Point>() {
                  @Override
                  public Point get() {
                    Point heroPoint =
                        new Point(getHeroPosition().position().x, getHeroPosition().position().y);
                    heroPoint.x -= 1;
                    return heroPoint;
                  }
                }),
            1);
    fireball.execute(hero);
    waitDelta();
  }

  /** Throw a fireball to the right. */
  public static void fireballRight() {
    Skill fireball =
        new Skill(
            new FireballSkill(
                new Supplier<Point>() {
                  @Override
                  public Point get() {
                    Point heroPoint =
                        new Point(getHeroPosition().position().x, getHeroPosition().position().y);
                    heroPoint.x += 1;
                    return heroPoint;
                  }
                }),
            1);
    fireball.execute(hero);
    waitDelta();
  }

  /*public static void interact() {
      InteractionTool.interactWithClosestInteractable(hero);
      waitDelta();
  }*/
}

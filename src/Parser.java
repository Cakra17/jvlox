package src;

import java.beans.Expression;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Recursive decent parser
 * 
 * this is top-down level parser that because it start from the outermost
 * rule which is expression into nested subexpression.
 * 
 * hierarchy of grammar rule (top to down)
 * 1. expression
 * 2. comparison
 * 3. term (addition and subtraction)
 * 4. factor (multiplication and division)
 * 5. unary (negate, negative)
 * 6. primary (parenthesis, number, string, boolean, nil, variable)
 * 
 * 
 */

class Parser {
  private static class ParseError extends RuntimeException {
  }

  private final List<Token> tokens;
  private int current = 0;

  Parser(List<Token> tokens) {
    this.tokens = tokens;
  }

  List<Stmt> parse() {
    List<Stmt> stmts = new ArrayList<>();

    while (!isAtEnd()) {
      stmts.add(declaration());
    }

    return stmts;
  }

  private Expr expression() {
    return assignment();
  }

  private Stmt declaration() {
    try {
      if (match(TokenType.VAR))
        return varDeclaration();
      return statement();
    } catch (ParseError e) {
      syncronize();
      return null;
    }
  }

  private Stmt varDeclaration() {
    Token name = consume(TokenType.IDENTIFIER, "Expect variable name.");
    Expr expr = null;
    if (match(TokenType.EQUAL))
      expr = expression();
    consume(TokenType.SEMICOLON, "Expect ; after variable declaration");
    return new Stmt.Var(name, expr);
  }

  private Stmt statement() {
    if (match(TokenType.FOR))
      return forStatement();
    if (match(TokenType.IF))
      return ifStatement();
    if (match(TokenType.PRINT))
      return printStatement();
    if (match(TokenType.WHILE))
      return whileStatement();

    if (match(TokenType.LEFT_BRACE))
      return new Stmt.Block(block());
    return expressionStatement();
  }

  private List<Stmt> block() {
    List<Stmt> stmts = new ArrayList<>();
    while (!check(TokenType.RIGHT_BRACE) && !isAtEnd()) {
      stmts.add(declaration());
    }
    consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
    return stmts;
  }

  private Stmt ifStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(TokenType.ELSE))
      elseBranch = statement();
    return new Stmt.If(condition, thenBranch, elseBranch);
  }

  private Stmt forStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer;
    if (match(TokenType.SEMICOLON)) {
      initializer = null;
    } else if (match(TokenType.VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(TokenType.SEMICOLON))
      condition = expression();

    consume(TokenType.SEMICOLON, "Expect ';' after loop condition");

    Expr increment = null;
    if (!check(TokenType.RIGHT_PAREN))
      increment = expression();

    consume(TokenType.RIGHT_PAREN, "Expect ')' after for condition");

    Stmt body = statement();

    if (increment != null) {
      body = new Stmt.Block(
          Arrays.asList(body, new Stmt.Expression(increment)));

    }

    if (condition == null)
      condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

    if (initializer != null)
      body = new Stmt.Block(Arrays.asList(initializer, body));

    return body;
  }

  private Stmt whileStatement() {
    consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = expression();
    consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition");

    Stmt body = statement();
    return new Stmt.While(condition, body);
  }

  private Stmt printStatement() {
    Expr expr = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after value");
    return new Stmt.Print(expr);
  }

  private Stmt expressionStatement() {
    Expr expr = expression();
    consume(TokenType.SEMICOLON, "Expect ';' after value");
    return new Stmt.Expression(expr);
  }

  private Expr assignment() {
    Expr expr = or();

    if (match(TokenType.EQUAL)) {
      Token equal = previous();
      Expr left = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable) expr).name;
        return new Expr.Assign(name, left);
      }

      error(equal, "Invalid assignment target.");
    }

    return expr;
  }

  private Expr or() {
    Expr expr = and();

    while (match(TokenType.OR)) {
      Token operator = previous();
      Expr right = and();
      return new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = comma();

    while (match(TokenType.AND)) {
      Token operator = previous();
      Expr right = comma();
      return new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr comma() {
    if (match(TokenType.COMMA)) {
      throw error(previous(), "Left operand is missing");
    }

    Expr expr = ternary();

    while (match(TokenType.COMMA)) {
      Token operator = previous();
      Expr right = ternary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr ternary() {
    Expr expr = equality();

    if (match(TokenType.QUESTION)) {
      Token question = previous();
      Expr thenExpr = equality();

      if (match(TokenType.COLON)) {
        Token colon = previous();
        Expr elseExpr = ternary();
        expr = new Expr.Ternary(question, expr, thenExpr, colon, elseExpr);
      } else {
        throw error(peek(), "Expected : on ternary operation");
      }
    }

    return expr;
  }

  private Expr equality() {
    if (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      throw error(previous(), "Left operand is missing");
    }

    Expr expr = comparison();

    while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    if (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      throw error(previous(), "Left operand is missing");
    }

    Expr expr = term();

    while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    if (match(TokenType.PLUS)) {
      throw error(previous(), "Left operand is missing");
    }

    Expr expr = factor();

    while (match(TokenType.PLUS, TokenType.MINUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    if (match(TokenType.SLASH, TokenType.STAR)) {
      throw error(previous(), "Left operand is missing");
    }

    Expr expr = unary();

    while (match(TokenType.SLASH, TokenType.STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(TokenType.BANG, TokenType.MINUS)) {
      Token operator = previous();
      Expr right = primary();
      return new Expr.Unary(operator, right);
    }

    return primary();
  }

  private Expr primary() {
    if (match(TokenType.TRUE))
      return new Expr.Literal(true);
    if (match(TokenType.FALSE))
      return new Expr.Literal(false);
    if (match(TokenType.NIL))
      return new Expr.Literal(null);

    if (match(TokenType.NUMBER, TokenType.STRING)) {
      return new Expr.Literal(previous().literal);
    }

    if (match(TokenType.IDENTIFIER)) {
      return new Expr.Variable(previous());
    }

    if (match(TokenType.LEFT_PAREN)) {
      Expr expr = expression();
      consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    throw error(peek(), "Expect expression.");
  }

  private Token consume(TokenType type, String message) {
    if (check(type))
      return advance();
    throw error(peek(), message);
  }

  private ParseError error(Token token, String message) {
    Jvlox.error(token, message);
    return new ParseError();
  }

  private void syncronize() {
    advance();

    while (!isAtEnd()) {
      if (previous().type == TokenType.SEMICOLON)
        return;

      switch (peek().type) {
        case CLASS:
        case FUN:
        case FOR:
        case VAR:
        case IF:
        case WHILE:
        case PRINT:
        case RETURN:
          return;
      }

      advance();
    }
  }

  private Token previous() {
    return tokens.get(current - 1);
  }

  private Token advance() {
    if (!isAtEnd())
      current++;
    return previous();
  }

  private Token peek() {
    return tokens.get(current);
  }

  private boolean check(TokenType type) {
    if (isAtEnd())
      return false;
    return peek().type == type;
  }

  private boolean isAtEnd() {
    return peek().type == TokenType.EOF;
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }
    return false;
  }
}

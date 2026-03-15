package src;

import java.util.List;

abstract class Stmt {
  interface Visitor <T> {
    T visitExpressionStmt(Expression stmt);
    T visitPrintStmt(Print stmt);
  }

  static class Expression extends Stmt {
    Expression(Expr expression) {
      this.expression = expression;
    }

    @Override
    <T> T accept (Visitor<T> visitor) {
      return visitor.visitExpressionStmt(this);
    }

    final Expr expression;
  }
  static class Print extends Stmt {
    Print(Expr expression) {
      this.expression = expression;
    }

    @Override
    <T> T accept (Visitor<T> visitor) {
      return visitor.visitPrintStmt(this);
    }

    final Expr expression;
  }

  abstract <T> T accept(Visitor<T> visitor);
}

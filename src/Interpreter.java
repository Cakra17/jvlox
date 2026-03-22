package src;

import java.util.ArrayList;
import java.util.List;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    class BreakException extends RuntimeException {
    }

    private List<Double> list;
    private Environment environment = new Environment();
    private boolean isREPL;

    Interpreter(boolean isREPL) {
        this.isREPL = isREPL;
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements)
                execute(statement);
        } catch (RuntimeError e) {
            Jvlox.runtimeError(e);
        }
    }

    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

            case GREATER:
                checkMixOperands(expr.operator, left, right);
                list = getValues(left, right);
                return list.get(0) > list.get(1);
            case GREATER_EQUAL:
                checkMixOperands(expr.operator, left, right);
                list = getValues(left, right);
                return list.get(0) >= list.get(1);
            case LESS:
                checkMixOperands(expr.operator, left, right);
                list = getValues(left, right);
                return list.get(0) < list.get(1);
            case LESS_EQUAL:
                checkMixOperands(expr.operator, left, right);
                list = getValues(left, right);
                return list.get(0) <= list.get(1);

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                if ((double) right == 0)
                    throw new RuntimeError(expr.operator, "Cannot divide by zero");
                return (double) left / (double) right;
            case PLUS:
                checkMixOperands(expr.operator, left, right);
                if (left instanceof Double && right instanceof Double)
                    return (double) left + (double) right;
                if (left instanceof String && right instanceof String)
                    return (String) left + (String) right;

                if (left instanceof String && right instanceof Double)
                    return (String) left + String.format("%.0f", right);
                else if (left instanceof Double && right instanceof String)
                    return String.format("%.0f", left) + (String) right;
        }

        return null;
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left))
                return left;
        } else {
            if (!isTruthy(left))
                return left;
        }

        return evaluate(expr.right);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double) right;

        }

        return null;
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double)
            return;
        throw new RuntimeError(operator, "Operand must be a number");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double)
            return;
        throw new RuntimeError(operator, "Operator must be numbers");
    }

    private void checkMixOperands(Token operator, Object left, Object right) {
        if ((left instanceof Double || left instanceof String) &&
                (right instanceof Double || right instanceof String))
            return;
        throw new RuntimeError(
                operator,
                "Incorrect Type of operand for this operation, use either string or number");
    }

    private List<Double> getValues(Object left, Object right) {
        List<Double> pair = new ArrayList<>();
        if (left instanceof Double) {
            pair.add((Double) left);
        } else {
            pair.add((double) String.valueOf(left).length());
        }

        if (right instanceof Double) {
            pair.add((Double) right);
        } else {
            pair.add((double) String.valueOf(right).length());
        }

        return pair;
    }

    @Override
    public Object visitTernaryExpr(Expr.Ternary expr) {
        Object left = evaluate(expr.thenBranch);
        Object right = evaluate(expr.elseBranch);
        boolean cond = isTruthy(evaluate(expr.condition));

        return cond ? left : right;
    }

    /*
     * TODO: think another alternative solution for challange 2
     */
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        Object value = environment.get(expr.name);
        if (value == null)
            throw new RuntimeError(expr.name, "Variable uninitialized");
        return value;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);
        environment.assign(expr.name, value);
        return value;
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private Void execute(Stmt stmt) {
        return stmt.accept(this);
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        Object value = evaluate(stmt.expression);
        if (isREPL)
            System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            return execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            return execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitBreakStmt(Stmt.Break stmt) {
        throw new BreakException();
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            try {
                execute(stmt.body);
            } catch (BreakException e) {
                break;
            }
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    private void executeBlock(List<Stmt> stmts, Environment env) {
        Environment previous = this.environment;
        try {
            this.environment = env;

            for (Stmt statement : stmts) {
                execute(statement);
            }
        } finally {
            this.environment = previous;
        }
    }

    private boolean isTruthy(Object obj) {
        if (obj == null)
            return false;
        if (obj instanceof Boolean)
            return (boolean) obj;
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        if (left == null && right == null)
            return true;
        if (left == null)
            return false;

        return left.equals(right);
    }

    private String stringify(Object obj) {
        if (obj == null)
            return "nil";

        if (obj instanceof Double) {
            String text = obj.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return obj.toString();
    }
}

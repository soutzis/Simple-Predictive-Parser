import java.io.IOException;
import java.text.MessageFormat;

/**
 * @author Petros Soutzis
 */
public class SyntaxAnalyser extends AbstractSyntaxAnalyser {

    private final String INV_TKN_ERR = "Got invalid token: {}"; // Invalid Token Error message format
    private final String XPCTD_OTHR_TKN = "Invalid token detected! Expected: {0}, but got {1} instead"; //Expected other

    SyntaxAnalyser(String filename) throws IOException {

        this.lex = new LexicalAnalyser(filename);
    }

    @Override
    public void _statementPart_() throws IOException, CompilationException {
        final String nonTerminalName = "StatementPart";

        myGenerate.commenceNonterminal(nonTerminalName); // Indicate start of Non-terminal recursion
        acceptTerminal(Token.beginSymbol); // first thing to do is accept the begin symbol (terminal)
        _statementList_();
        acceptTerminal(Token.endSymbol); // last thing to do is to accept the end symbol (terminal)
        myGenerate.finishNonterminal(nonTerminalName); // Indicate end of Non-terminal recursion
    }

    private void _statementList_() throws IOException, CompilationException{
        final String nonTerminalName = "StatementList";
        myGenerate.commenceNonterminal(nonTerminalName);

        _statement_(); //any errors for invalid tokens will be caught in statement method

        if (nextToken.symbol == Token.semicolonSymbol) {
            acceptTerminal(Token.semicolonSymbol);
            _statementList_();
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    private void _statement_() throws IOException, CompilationException {
        final String nonTerminalName = "Statement";
        myGenerate.commenceNonterminal(nonTerminalName);

        switch(nextToken.symbol){
            case Token.identifier:_assignmentStatement_();break;
            case Token.ifSymbol: _ifStatement_();break;
            case Token.whileSymbol: _whileStatement_();break;
            case Token.callSymbol: _procedureStatement_();break;
            case Token.doSymbol: _untilStatement_();break;
            case Token.forSymbol: _forStatement_();break;

            default: myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));
        }
        myGenerate.finishNonterminal(nonTerminalName);
    }

    private void _assignmentStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "AssignmentStatement";
        myGenerate.commenceNonterminal(nonTerminalName);

        acceptTerminal(Token.identifier);
        acceptTerminal(Token.becomesSymbol);

        int symbol = nextToken.symbol;

        if(symbol == Token.identifier || symbol == Token.numberConstant || symbol == Token.leftParenthesis)
            _expression_();
        else if(symbol == Token.stringConstant)
            acceptTerminal(Token.stringConstant);
        else
            myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));

        myGenerate.finishNonterminal(nonTerminalName);
    }

    //TODO
    private void _ifStatement_() throws IOException, CompilationException {
        final String nonTerminalName = "IfStatement";
        myGenerate.commenceNonterminal(nonTerminalName);
        /*1. Accept "if" token*/
        acceptTerminal(Token.ifSymbol);
        /*2. Get the condition and ultimately accept the identifier OR throw an error*/
        if(nextToken.symbol == Token.identifier)
            _condition_();
        else
            myGenerate.reportError(nextToken,
                    MessageFormat.format(XPCTD_OTHR_TKN, Token.getName(Token.identifier), nextToken.toString()));
        /*3. Accept the "then" token*/
        acceptTerminal(Token.thenSymbol);
        /*4. Call statementList*/
        _statementList_();

        /*5. Check if nextToken is to end the if statement or if it is an else clause.
        * 6. Finally accept the tokens that mark the end of the if statement, if not already accepted.*/
        switch (nextToken.symbol){
            case Token.endSymbol:
                acceptTerminal(Token.endSymbol);
                acceptTerminal(Token.ifSymbol);
                break;
            case Token.elseSymbol:
                acceptTerminal(Token.elseSymbol);
                _statementList_();
                acceptTerminal(Token.endSymbol);
                acceptTerminal(Token.ifSymbol);
                break;
        }

        myGenerate.finishNonterminal(nonTerminalName);

    }

    //TODO
    private void _procedureStatement_(){

        final String nonTerminalName = "";
    }

    //TODO
    private void _whileStatement_(){
        final String nonTerminalName = "";

    }

    //TODO
    private void _untilStatement_(){
        final String nonTerminalName = "";

    }

    //TODO
    private void _forStatement_(){
        final String nonTerminalName = "";

    }

    //TODO
    private void _argumentList_(){
        final String nonTerminalName = "";

    }

    //TODO
    private void _condition_(){
        final String nonTerminalName = "";

    }

    //TODO
    private void _conditionalOperator_(){
        final String nonTerminalName = "";

    }

    private void _expression_() throws IOException, CompilationException {
        final String nonTerminalName = "Expression";
        myGenerate.commenceNonterminal(nonTerminalName);
        int symbol = nextToken.symbol;

        if(symbol == Token.identifier || symbol == Token.numberConstant || symbol == Token.leftParenthesis)
            _term_();
        else if(symbol == Token.plusSymbol){
            acceptTerminal(Token.plusSymbol);
            _expression_();
        }
        else if(symbol == Token.minusSymbol){
            acceptTerminal(Token.minusSymbol);
            _expression_();
        }
        else
            myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));

        myGenerate.finishNonterminal(nonTerminalName);

    }

    private void _term_() throws IOException, CompilationException {
        final String nonTerminalName = "Term";
        myGenerate.commenceNonterminal(nonTerminalName);
        int symbol = nextToken.symbol;

        if(symbol == Token.identifier || symbol == Token.numberConstant || symbol == Token.leftParenthesis)
            _factor_();
        else if(symbol == Token.timesSymbol){
            acceptTerminal(Token.timesSymbol);
            _term_();
        }
        else if(symbol == Token.divideSymbol){
            acceptTerminal(Token.divideSymbol);
            _term_();
        }
        else
            myGenerate.reportError(nextToken, MessageFormat.format(INV_TKN_ERR, nextToken.toString()));
        myGenerate.finishNonterminal(nonTerminalName);
    }

    private void _factor_() throws IOException, CompilationException {
        final String nonTerminalName = "Factor";
        myGenerate.commenceNonterminal(nonTerminalName);

        switch (nextToken.symbol){
            case Token.identifier: acceptTerminal(Token.identifier); break;
            case Token.numberConstant: acceptTerminal(Token.numberConstant); break;
            case Token.leftParenthesis:
                acceptTerminal(Token.leftParenthesis); // accept left parenthesis if one is read
                _expression_(); //call expression, but when the (SP) returns, expect a right parenthesis for each call!
                acceptTerminal(Token.rightParenthesis);
                break;
        }
        myGenerate.finishNonterminal(nonTerminalName);

    }

    @Override
    public void acceptTerminal(int symbol) throws IOException, CompilationException {
        int examinedSymbol = nextToken.symbol;

        if (examinedSymbol == symbol){
            myGenerate.insertTerminal(nextToken); //print terminal name to output
            nextToken = lex.getNextToken();  //get next token from lexical analyser
        }
        else
            myGenerate.reportError(nextToken,
                    MessageFormat.format(XPCTD_OTHR_TKN, Token.getName(symbol), nextToken.toString()));
    }
}

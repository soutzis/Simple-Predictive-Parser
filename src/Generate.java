/**
 * @author Petros Soutzis
 */
@SuppressWarnings("Duplicates")
public class Generate extends AbstractGenerate {

    private int indentationLevel;

    Generate(){

        this.indentationLevel = 0; // initialise to 0, as it is the start of the program
    }


    @Override
    public void commenceNonterminal( String name ) {
        addIndentation(indentationLevel);
        super.commenceNonterminal(name);
        this.indentationLevel++;
    } // end of method commenceNonterminal

    @Override
    public void addVariable(Variable v) {
        addIndentation(indentationLevel);
        super.addVariable(v);
    }

    @Override
    public void insertTerminal( Token token ) {
        addIndentation(indentationLevel); // print tabs equal to the number of indentation
        super.insertTerminal(token);
    } // end of method insertTerminal


    @Override
    public void finishNonterminal( String name ) {
        this.indentationLevel--;
        addIndentation(indentationLevel);
        super.finishNonterminal(name);
    } // end of method finishNonterminal

    @Override
    public void reportError(Token token, String explanatoryMessage) throws CompilationException {

        throw new CompilationException(explanatoryMessage, token.lineNumber);
    }

    /**
     * This method will print a tab if indentation level > 0. Otherwise it will do nothing!
     * @param level the current indentation level. E.g if level is 2, then 2 tabs will be printed, etc.
     */
    private void addIndentation(int level){
        for(int i=0; i<level; i++)
            System.out.print("\t"); // print a tab 'n' times, where n = level
    }
}

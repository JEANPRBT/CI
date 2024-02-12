
/**
 * @author Rickard Cornell, Elissa Arias Sosa, Raahitya Botta, Zaina Ramadan, Jean Perbet
 * Class representing our CI server which handles all incoming webhooks using HTTP methods.
 */
public final class Add {
    private int a;
    private int  b;
    
    public Add(int a, int b){
         this.a = a;
         this.b = b; 

    }
    public int getRes(){
        return add(a, b);
    }

    private int add(int a, int b){
        return a + b;
    }
}

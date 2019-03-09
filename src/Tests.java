public class Tests {

    public static int[] array = {1,2,3,4,5,6,7,8,9,21};

    private static void recursive (int x){

        System.out.println("START print number: "+x);
        if(x < 6)
            recursive(x *10);
        System.out.println("STOP print number: "+x);
    }

    public static void main(String[] args){
        for (int i=0; i<array.length; i++)
            recursive(array[i]);
    }


}

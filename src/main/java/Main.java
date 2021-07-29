import javax.persistence.*;
import java.util.*;
import java.util.concurrent.Callable;

public class Main {

    private static EntityManager em;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("Menu");
        em = emf.createEntityManager();
        init();
    }

    public static void init(){
        while(true) {
            System.out.println("1 -> show menu");
            System.out.println("2 -> add food");
            System.out.println("3 -> add random food");
            String choice = scanner.nextLine();
            switch (choice){
                case "1":
                    showFood();
                    break;
                case "2":
                    addFood();
                    break;
                case "3":
                    addRandomFood();
                    break;
                default:
                    System.exit(0);
            }
        }
    }

    public static <T> T performTransaction(Callable<T> action){
        EntityTransaction transaction = em.getTransaction();
        transaction.begin();
        try{
            T result = action.call(); // throws java.lang.Exception

            transaction.commit();

            return result;
        }catch (Exception e){
            if(transaction.isActive()){
                transaction.rollback();
            }

            throw new RuntimeException();
        }
    }

    public static void addFood(){
        System.out.println("Enter name:");
        String name = scanner.nextLine();
        System.out.println("Enter price:");
        String price = scanner.nextLine();
        System.out.println("Enter weight:");
        String weight = scanner.nextLine();
        System.out.println("Enter if on sale (yes or no):");
        String onSale = scanner.nextLine();

        Food food = new Food(name, Double.parseDouble(price), Double.parseDouble(weight), onSale.equals("yes"));

        performTransaction(() -> {
           em.persist(food);
           return em;
        });
    }

    public static void addRandomFood(){
        Random random = new Random();
        System.out.println("Enter count:");
        String str = scanner.nextLine();
        int count = Integer.parseInt(str);
        performTransaction(() -> {
            for(int i=0; i<count; i++){
                Food food = new Food("Food" + i, random.nextDouble() * 100, random.nextDouble(), random.nextBoolean());
                em.persist(food);
            }
            return null;
        });
    }

    public static void showFood(){
        String select = "SELECT f FROM Food f";

        TypedQuery<Food> query;

        printSort();
        String choice = scanner.nextLine();

        switch (choice){
            case "1":
                query = em.createQuery(select, Food.class);
                break;
            case "2":
                query = showByPrice(select);
                break;
            case "3":
                query = em.createQuery(select + " WHERE f.onSale = true", Food.class);
                break;
            case "4":
                query = em.createQuery(select, Food.class);
                for(List<Food> list : showLessThanKG(query.getResultList())) {
                    System.out.println("\t ****************************************************");
                    printFood(list);
                }
                return;
            default:
                return;
        }

        printFood(query.getResultList());
    }

    private static void printSort(){
        System.out.println("Select sorting method:");
        System.out.println("1 -> View all");
        System.out.println("2 -> Price");
        System.out.println("3 -> On sale");
        System.out.println("4 -> Combo menu less than 1 kg");
    }

    private static void printFood(List<Food> foods){
        for(Food food : foods){
            System.out.println(food);
        }
    }

    private static List<List<Food>> showLessThanKG(List<Food> foods){
        List<List<Food>> result = new ArrayList<>();

        int length = foods.size();

        for(int i=0; i<length-1; i++){
            Food initial = foods.get(i);
            double weight = initial.getWeight();
            List<Food> lessThanKG = new ArrayList<>();
            lessThanKG.add(initial);
            for(int j=i+1; j<length; j++){
                Food secondary = foods.get(j);
                double secWeight = secondary.getWeight();
                if(weight + secWeight > 1){
                    continue;
                }
                weight += secWeight;
                lessThanKG.add(secondary);
            }
            result.add(lessThanKG);
        }
        return result;
    }

    private static TypedQuery<Food> showByPrice(String select){
        System.out.println("Enter minimum price:");
        String strmin = scanner.nextLine();
        Double min = Double.parseDouble(strmin);
        System.out.println("Enter maximum price:");
        String strmax = scanner.nextLine();
        Double max = Double.parseDouble(strmax);
        TypedQuery<Food> typedQuery = em.createQuery(select + " WHERE f.price BETWEEN :min AND :max", Food.class);
        typedQuery.setParameter("min", min);
        typedQuery.setParameter("max", max);
        return typedQuery;
    }
}

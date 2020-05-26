package com.example.myapplication;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.util.Pair;
import android.view.PointerIcon;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import static java.lang.Math.abs;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void onButtonClick(View v) {
        EditText Equals = findViewById(R.id.equals);
        EditText populationNumber = findViewById(R.id.populationNumber);
        EditText A = findViewById(R.id.editA);
        EditText B = findViewById(R.id.editB);
        EditText C = findViewById(R.id.editC);
        EditText D = findViewById(R.id.editD);
        EditText MinText = findViewById(R.id.minText);
        EditText MaxText = findViewById(R.id.maxText);

        TextView resultText = findViewById(R.id.resultText);
        TextView deltaText = findViewById(R.id.delta);
        TextView timeText = findViewById(R.id.timeText);
        TextView bestPercent = findViewById(R.id.bestPercent);

        if (A.getText().toString().trim().equals("")
                || B.getText().toString().trim().equals("")
                || C.getText().toString().trim().equals("")
                || D.getText().toString().trim().equals("")
                || Equals.getText().toString().trim().equals("")
                || populationNumber.getText().toString().trim().equals("")
                || MinText.getText().toString().trim().equals("")
                || MaxText.getText().toString().trim().equals("")) {
            resultText.setText("Введіть вірні дані!");
            return;
        }
        if (populationNumber.getText().toString().trim().equals("1")) {
            resultText.setText("Кількість популяцій повинна бути більшою за 1");
            return;
        }

        int Y = Integer.parseInt(Equals.getText().toString());
        int POPULATION_SIZE = Integer.parseInt(populationNumber.getText().toString());
        int a = Integer.parseInt(A.getText().toString());
        int b = Integer.parseInt(B.getText().toString());
        int c = Integer.parseInt(C.getText().toString());
        int d = Integer.parseInt(D.getText().toString());
        int GENE_MIN = Integer.parseInt(MinText.getText().toString());
        int GENE_MAX = Integer.parseInt(MaxText.getText().toString());

        if (GENE_MIN >= GENE_MAX) {
            resultText.setText("Виникла помилка: мінімум не може перевищувати максимум");
            return;
        }
        long m = System.currentTimeMillis();
        FunctionToSolve func = new FunctionToSolve(a,b,c,d,Y);
        GeneticAlgorithm ga = new GeneticAlgorithm(GENE_MAX,GENE_MIN,func, POPULATION_SIZE);
        Chromosome result = ga.Start();
        StringBuilder text;
        StringBuilder delta;
        if (ga.isPrecise) {
            text = new StringBuilder("Результат: ");
            delta = new StringBuilder("Похибка: 0");
        } else{
            text = new StringBuilder("Наближений результат: ");
            delta = new StringBuilder(String.format("Похибка: %d", result.fitness));
        }
        for (int i = 0; i < result.genes.length; i++) {
            text.append(String.format("x%d = %d; ", i+1, result.genes[i]));
        }
        resultText.setText(text);
        deltaText.setText(delta);
        bestPercent.setText("Відсоток мутацій: " + ga.mutation_percent);
        timeText.setText("Час виконання: " + (System.currentTimeMillis() - m) + " ms");
    }
}

class FunctionToSolve {
    int argsCount = 4;
    private int a, b, c, d, Y;
    public FunctionToSolve(int a, int b, int c, int d, int Y){
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.Y = Y;
    }
    public int calculate(int x1, int x2, int x3, int x4){
        return x1*a + x2*b + x3*c + x4*d - Y;
    }
}

class Chromosome{
    int fitness;
    int[] genes;
    public Chromosome(int numOfGenes){
        this.genes = new int[numOfGenes];
    }
}

@RequiresApi(api = Build.VERSION_CODES.N)
class GeneticAlgorithm{
    public boolean isPrecise;
    private int Max;
    private int Min;
    private int POPULATION_SIZE;
    private int CHROMOSOME_SIZE;
    private Chromosome[] population;
    private FunctionToSolve Function;
    public float mutation_percent;
    private Chromosome closestSolution;
    private Random random = new Random();

    public GeneticAlgorithm(int max, int min, FunctionToSolve func, int population_size){
        Max = max;
        Min = min;
        Function = func;
        CHROMOSOME_SIZE = Function.argsCount;
        POPULATION_SIZE = population_size;
        population = new Chromosome[POPULATION_SIZE];
    }

    public Chromosome Start(){
        initializePopulation();
        while (mutation_percent < 100) {
            for (int j = 0; j < 10; j++) {
                sortByFitness();
                if(population[0].fitness == 0){
                    isPrecise = true;
                    return population[0];
                }else if(closestSolution == null || closestSolution.fitness > population[0].fitness){
                    closestSolution = population[0];
                }
                selectPopulation();
                crossovers();
                mutations(mutation_percent);
            }
            mutation_percent+= 2.5F;
        }
        isPrecise = false;
        return closestSolution;
    }

    private void initializePopulation() {
        for (int i = 0; i < POPULATION_SIZE; i++) {
            population[i] = new Chromosome(CHROMOSOME_SIZE);
            randomizeGenes(population[i]);
            calcFitness(population[i]);
        }
    }

    private void calcFitness(Chromosome chr){
        chr.fitness = abs(Function.calculate(chr.genes[0],chr.genes[1],chr.genes[2],chr.genes[3]));
    }

    private void randomizeGenes(Chromosome chr){
        for (int i = 0; i < CHROMOSOME_SIZE; i++) {
            chr.genes[i] = randGene();
        }
    }

    private int randGene(){
        return random.nextInt(Max-Min) + Min;
    }

    private void sortByFitness(){
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Arrays.sort(this.population, (chr1,chr2) -> chr1.fitness - chr2.fitness);
        }
    }

    private void selectPopulation(){
        int selN = random.nextInt(2);
        if(selN == 0){
            roulette();
        }
        else {
            tournament();
        }
    }

    private void crossovers(){
        Chromosome[] newPopulation = new Chromosome[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE/2; i++) {
            Pair<Chromosome,Chromosome> childs;
            int co = random.nextInt(2);
            if(co == 0){
                childs = monoCrossover(population[i], population[i+POPULATION_SIZE/2]);
            }else{
                childs = duoCrossover(population[i], population[i+POPULATION_SIZE/2]);
            }
            newPopulation[i] = childs.first;
            newPopulation[i+POPULATION_SIZE/2] = childs.second;
        }
        population = newPopulation;
    }

    private void mutations(float mutPercent){
        for (int i = 0; i < POPULATION_SIZE; i++) {
            float prob = random.nextFloat() * 100;
            if(prob < mutPercent){
                population[i].genes[random.nextInt(CHROMOSOME_SIZE)] = randGene();
                calcFitness(population[i]);
            }
        }
    }

    private Pair<Chromosome,Chromosome> monoCrossover(Chromosome chr1, Chromosome chr2){
        int line = randLine(0);
        Chromosome chrChild1 = new Chromosome(CHROMOSOME_SIZE);
        Chromosome chrChild2 = new Chromosome(CHROMOSOME_SIZE);
        for (int i = 0; i < CHROMOSOME_SIZE; i++) {
            if(i<=line){
                chrChild1.genes[i] = chr1.genes[i];
                chrChild2.genes[i] = chr2.genes[i];
            }
            else{
                chrChild1.genes[i] = chr2.genes[i];
                chrChild2.genes[i] = chr1.genes[i];
            }
        }
        calcFitness(chrChild1);
        calcFitness(chrChild2);
        return Pair.create(chrChild1,chrChild2);
    }

    private Pair<Chromosome,Chromosome> duoCrossover(Chromosome chr1, Chromosome chr2){
        int line1 = randLine(1);
        int line2 = randLine(2);
        Chromosome chrChild1 = new Chromosome(CHROMOSOME_SIZE);
        Chromosome chrChild2 = new Chromosome(CHROMOSOME_SIZE);
        for (int i = 0; i < CHROMOSOME_SIZE; i++) {
            if(i<=line1||i>line2){
                chrChild1.genes[i] = chr1.genes[i];
                chrChild2.genes[i] = chr2.genes[i];
            }
            else{
                chrChild1.genes[i] = chr2.genes[i];
                chrChild2.genes[i] = chr1.genes[i];
            }
        }
        calcFitness(chrChild1);
        calcFitness(chrChild2);
        return Pair.create(chrChild1,chrChild2);
    }

    private int randLine(int part){
        int line;
        if (part == 0){
            line = random.nextInt(CHROMOSOME_SIZE-1);
        }else if (part == 1){
            line = random.nextInt((CHROMOSOME_SIZE+1)/2-1);
        }else {
            line = random.nextInt((CHROMOSOME_SIZE-1)/2) + (CHROMOSOME_SIZE+1)/2;
        }
        return line;
    }

//    private Pair<Chromosome,Chromosome> collisionCrossover(Chromosome chr1, Chromosome chr2){
//        Chromosome chrChild1 = new Chromosome(CHROMOSOME_SIZE);
//        Chromosome chrChild2 = new Chromosome(CHROMOSOME_SIZE);
//        int v1,v2;
//        Function<Chromosome, Integer> calcV = (chr) -> {
//            int v = 0;
//            for (int i = 0; i < CHROMOSOME_SIZE; i++) {
//                v+= chr.genes[i];
//            }
//            return v;
//        };
//        v1 = calcV.apply(chr1);
//        v2 = calcV.apply(chr2);
//        for (int i = 0; i < CHROMOSOME_SIZE; i++) {
//            float V1, V2;
//            V1 = (float)(chr1.genes[i] - chr2.genes[i])/(chr1.genes[i] + chr2.genes[i])*v1 + (float)2*chr2.genes[i]/(chr1.genes[i] + chr2.genes[i])*v2;
//            V2 = (float) 2*chr1.genes[i]/(chr1.genes[i] + chr2.genes[i])*v1 - (float)(chr1.genes[i] - chr2.genes[i])/(chr1.genes[i] + chr2.genes[i])*v2;
//            if(V1>0){
//                chrChild1.genes[i] = chr2.genes[i];
//            } else{
//                chrChild1.genes[i] = chr1.genes[i];
//            }
//            if(V2>0){
//                chrChild2.genes[i] = chr1.genes[i];
//            }else{
//                chrChild2.genes[i] = chr2.genes[i];
//            }
//        }
//        calcFitness(chrChild1);
//        calcFitness(chrChild2);
//        return Pair.create(chrChild1,chrChild2);
//    }

    private void roulette(){
        float coef=0;
        float prevSum=0;
        Chromosome[] chrs = new Chromosome[POPULATION_SIZE];
        float[] chances = new float[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            coef += 1.0/population[i].fitness;
        }
        for(int i = 0; i < POPULATION_SIZE;i++){
            float range = (float) 1.0/population[i].fitness/coef + prevSum;
            chances[i] = range;
            prevSum = range;
        }
        for (int i = 0; i < POPULATION_SIZE; i++) {
            float prob = random.nextFloat();
            for(int j = 0; j < POPULATION_SIZE; j++){
                if(prob < chances[j]){
                    chrs[i] = population[j];
                    break;
                }
            }
        }
        population = chrs;
    }

    private void tournament(){
        Chromosome[] chrs = new Chromosome[POPULATION_SIZE];
        for (int i = 0; i < POPULATION_SIZE; i++) {
            Chromosome chr1 = population[i];
            Chromosome chr2 = population[random.nextInt(POPULATION_SIZE)];
            chrs[i] = chr1.fitness > chr2.fitness? chr2:chr1;
        }
        population = chrs;
    }
}

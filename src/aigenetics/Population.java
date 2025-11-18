package aigenetics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Gestisce una collezione di individui (HeuristicWeights).
 */
public class Population {
    
    public List<HeuristicWeights> individuals;

    public Population(int size, boolean initialize) {
        this.individuals = new ArrayList<>(size);
        if (initialize) {
            for (int i = 0; i < size; i++) {
                this.individuals.add(new HeuristicWeights());
            }
        }
    }

    public void add(HeuristicWeights individual) {
        this.individuals.add(individual);
    }

    public int size() {
        return this.individuals.size();
    }

    public HeuristicWeights get(int index) {
        return this.individuals.get(index);
    }

    public HeuristicWeights getFittest() {
        return this.individuals.stream()
                   .max(Comparator.comparingDouble(ind -> ind.fitness))
                   .orElse(null);
    }
}
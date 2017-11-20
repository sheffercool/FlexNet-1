class NetTrainer(val stepOfJCheck: Int = 50) {

    val MAX_TRIES = 500
    var triesCounter: Int = 0
    val MIN_J_DIFFERENCE_PERCENTAGE = 0.00001
    val MIN_GOOD_J = 0.55

    fun resetTrainingTriesCount() {
        triesCounter = 0
    }

    /**
     * Trains a data set and returns if the training is done (true) or should train more (false)
     */
    fun trainFolding(flexNet: FlexNet, folds: MutableList<Fold>, testFold: Int) : Boolean {
        var previousJ: Double
        var newJ: Double

        //add instances from training folds to one big list of instances called trainingInstances
        val trainingFolds = folds.filterIndexed{ index, _ -> index != testFold - 1 }
        val trainingInstances = mutableListOf<Instance>()
        trainingFolds.forEach { it.dataSet.forEach { trainingInstances.add(it) } }

        //separate instances in (trainingInstances.size/stepOfJCheck) small batches
        (0 until (trainingInstances.size/stepOfJCheck)).forEach {
            //train batch
            previousJ = flexNet.calculateJ(folds, testFold)
            val batch = buildBatch(trainingInstances, it)
            trainBatch(flexNet, batch)
            newJ = flexNet.calculateJ(folds, testFold)

            //after each batch is trained, checks if should end training
            if (shouldEndTraining(previousJ, newJ)) return true
        }

        //23-3 = 20                                          __ __ __
        //0 1 2 3 4 5 6 7 8  9 10 11 12 13 14 15 16 17 18 19 20 21 22
        //1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20 21 22 23

        //trains rest of instances that didn't fit in batches (if there is any)
        val remainingBatch = trainingInstances.subList(trainingInstances.size - (trainingInstances.size % stepOfJCheck), trainingInstances.lastIndex)
        if (remainingBatch.isNotEmpty()) {
            previousJ = flexNet.calculateJ(folds, testFold)
            trainBatch(flexNet, remainingBatch)
            newJ = flexNet.calculateJ(folds, testFold)

            //after rest of instances are trained, checks if should end training
            if (shouldEndTraining(previousJ, newJ)) return true
        }

        //should train more, training is not done and training set was fully used
        return false
    }

    /**
     * Trains a batch of instances
     */
    fun trainBatch(flexNet: FlexNet, batch: List<Instance>) {
        batch.forEach{
            flexNet.forthAndBackPropagate(it)
            flexNet.updateThetas()
        }
    }

    /**
     * Builds a sublist of size stepOfJCheck, given a batch number, which determines where to cut the list
     */
    private fun buildBatch(instances: List<Instance>, batchNumber: Int): List<Instance> =
            instances.subList(batchNumber*stepOfJCheck, (batchNumber+1)*stepOfJCheck-1)

    private fun shouldEndTraining(previousJ: Double, newJ: Double): Boolean {
        return isJGoodEnough(previousJ, newJ) or triedEnoughTimes()
    }

    private fun isJGoodEnough(previousJ: Double, newJ: Double): Boolean {
        if (newJ <= MIN_GOOD_J) {
            println("Good J = $newJ")
            return true

        } else if (Math.abs((previousJ - newJ)/previousJ) <= MIN_J_DIFFERENCE_PERCENTAGE) {
            println("J difference percentage = ${Math.abs((previousJ - newJ)/previousJ)}")
            return true
        } else
            return false

    }

    private fun triedEnoughTimes(): Boolean {
        triesCounter++
        if (triesCounter >= MAX_TRIES) {
            println("triesCounter = $triesCounter")
            return true
        } else
            return false

    }

}

fun main(args: Array<String>) {
    val config = FlexNetConfig(
            inputNeurons = 1,
            numberOfTargetAttributeClassesInDataSet = 2,
            hiddenLayers = 1,
            neuronsPerHiddenLayer = 3,
            alpha = 0.0001,
            lambda = 0.0
    )
    val flexNet = FlexNet(config)
    val netTrainer = NetTrainer()
    flexNet.print()
    val instance1 = Instance(mutableListOf(0.1), "Class1", 0)
    val instance2 = Instance(mutableListOf(0.2), "Class2", 1)
    val foldA = Fold(listOf(instance1, instance2))
    val instance3 = Instance(mutableListOf(0.3), "Class1", 0)
    val instance4 = Instance(mutableListOf(0.4), "Class2", 1)
    val foldB = Fold(listOf(instance3, instance4))
    val instance5 = Instance(mutableListOf(0.5), "Class1", 0)
    val instance6 = Instance(mutableListOf(0.6), "Class2", 1)
    val foldC = Fold(listOf(instance5, instance6))
    val folds = mutableListOf(foldA, foldB, foldC)

    netTrainer.trainFolding(flexNet, folds, 1)
}
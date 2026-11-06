package pl.ceranka.happypies.data.trainer

interface TrainerRepository {

    @Throws(Exception::class)
    suspend fun getTrainer(trainerId: String): Trainer
}
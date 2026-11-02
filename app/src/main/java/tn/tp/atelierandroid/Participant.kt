package tn.tp.atelierandroid

open class Personne(val nom: String, val prenom: String, var age: Int)

class Participant(
    nom: String,
    prenom: String,
    age: Int,
    val groupe: String,
    val support: String
) : Personne(nom, prenom, age) {
    fun fiche(): String {
        val categorie = when (age) {
            in 1..17 -> "Mineur"
            in 18..25 -> "Jeune adulte"
            else -> "Adulte"
        }
        return "Nom : $nom | Prénom : $prenom | Âge : $age ans | Catégorie : $categorie | Groupe : $groupe | Support : $support"
    }
}

package tn.tp.atelierandroid

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    private val participants = mutableListOf<Participant>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        val etNom = findViewById<EditText>(R.id.etNom)
        val etPrenom = findViewById<EditText>(R.id.etPrenom)
        val etAge = findViewById<EditText>(R.id.etAge)
        val spGroupe = findViewById<Spinner>(R.id.spGroupe)
        val rgSupport = findViewById<RadioGroup>(R.id.rgSupport)
        val cbConfirmation = findViewById<CheckBox>(R.id.cbConfirmation)
        val tvResultat = findViewById<TextView>(R.id.tvResultat)
        val tvCompteur = findViewById<TextView>(R.id.tvCompteur)
        val tvListe = findViewById<TextView>(R.id.tvListe)
        val etRecherche = findViewById<EditText>(R.id.etRecherche)

        fun lireParticipant(): Participant? {
            val nom = etNom.text.toString().trim()
            val prenom = etPrenom.text.toString().trim()
            val age = etAge.text.toString().trim().toIntOrNull()
            val groupe = spGroupe.selectedItem?.toString().orEmpty()
            val support = when (rgSupport.checkedRadioButtonId) {
                R.id.rbEmulateur -> "Émulateur"
                R.id.rbTelephone -> "Téléphone réel"
                else -> ""
            }

            when {
                nom.isEmpty() || prenom.isEmpty() -> "Veuillez saisir le nom et le prénom."
                age == null || age !in 1..120 -> "L'âge doit être compris entre 1 et 120."
                groupe == "Choisir un groupe" || groupe.isEmpty() -> "Veuillez choisir un groupe."
                support.isEmpty() -> "Veuillez choisir un support de test."
                !cbConfirmation.isChecked -> "Veuillez confirmer les informations."
                else -> return Participant(nom, prenom, age, groupe, support)
            }.also { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() }
            return null
        }

        fun actualiserCompteur() {
            tvCompteur.text = getString(R.string.compteur_inscriptions, participants.size)
        }

        findViewById<Button>(R.id.btnAfficher).setOnClickListener {
            lireParticipant()?.let { tvResultat.text = it.fiche() }
        }

        findViewById<Button>(R.id.btnEffacer).setOnClickListener {
            etNom.text.clear()
            etPrenom.text.clear()
            etAge.text.clear()
            spGroupe.setSelection(0)
            rgSupport.clearCheck()
            cbConfirmation.isChecked = false
            tvResultat.text = getString(R.string.resultat_defaut)
        }

        findViewById<Button>(R.id.btnEnregistrer).setOnClickListener {
            val participant = lireParticipant() ?: return@setOnClickListener
            val doublon = participants.any {
                it.nom.equals(participant.nom, ignoreCase = true) &&
                    it.prenom.equals(participant.prenom, ignoreCase = true)
            }
            if (doublon) {
                Toast.makeText(this, R.string.erreur_doublon, Toast.LENGTH_SHORT).show()
            } else {
                participants.add(participant)
                tvResultat.text = getString(R.string.inscription_reussie, participant.prenom, participant.nom)
                actualiserCompteur()
            }
        }

        findViewById<Button>(R.id.btnListe).setOnClickListener {
            tvListe.text = if (participants.isEmpty()) {
                getString(R.string.liste_vide)
            } else {
                participants.joinToString(separator = "\n\n") { it.fiche() }
            }
        }

        findViewById<Button>(R.id.btnRechercher).setOnClickListener {
            val nomRecherche = etRecherche.text.toString().trim()
            if (nomRecherche.isEmpty()) {
                Toast.makeText(this, R.string.recherche_vide, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            var trouve: Participant? = null
            for (participant in participants) {
                if (participant.nom.equals(nomRecherche, ignoreCase = true)) {
                    trouve = participant
                    break
                }
            }
            tvListe.text = trouve?.fiche() ?: getString(R.string.aucun_participant)
        }

        actualiserCompteur()
    }
}

package tn.tp.atelierandroid

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val participants = mutableListOf<Participant>()
    private val participantsAffiches = mutableListOf<Participant>()
    private var participantSelectionne: Participant? = null

    private lateinit var etNom: EditText
    private lateinit var etPrenom: EditText
    private lateinit var etAge: EditText
    private lateinit var spGroupe: Spinner
    private lateinit var rgSupport: RadioGroup
    private lateinit var cbConfirmation: CheckBox
    private lateinit var tvResultat: TextView
    private lateinit var tvCompteur: TextView
    private lateinit var spFiltreGroupe: Spinner
    private lateinit var lvParticipants: ListView
    private lateinit var btnModifier: Button
    private lateinit var btnSupprimer: Button
    private lateinit var btnAnnuler: Button

    private val tagCycle = "AtelierCycle"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(tagCycle, "onCreate")
        setContentView(R.layout.activity_main)

        etNom = findViewById(R.id.etNom)
        etPrenom = findViewById(R.id.etPrenom)
        etAge = findViewById(R.id.etAge)
        spGroupe = findViewById(R.id.spGroupe)
        rgSupport = findViewById(R.id.rgSupport)
        cbConfirmation = findViewById(R.id.cbConfirmation)
        tvResultat = findViewById(R.id.tvResultat)
        tvCompteur = findViewById(R.id.tvCompteur)
        spFiltreGroupe = findViewById(R.id.spFiltreGroupe)
        lvParticipants = findViewById(R.id.lvParticipants)
        btnModifier = findViewById(R.id.btnModifier)
        btnSupprimer = findViewById(R.id.btnSupprimer)
        btnAnnuler = findViewById(R.id.btnAnnuler)

        restaurerParticipants(savedInstanceState)
        spFiltreGroupe.setSelection(
            (savedInstanceState?.getInt(CLE_FILTRE, 0) ?: 0).coerceIn(0, 3)
        )
        actualiserAffichage()

        findViewById<Button>(R.id.btnAfficher).setOnClickListener {
            lireParticipant()?.let { tvResultat.text = it.fiche() }
        }
        findViewById<Button>(R.id.btnEffacer).setOnClickListener {
            reinitialiserFormulaire()
            tvResultat.text = getString(R.string.resultat_defaut)
        }
        findViewById<Button>(R.id.btnEnregistrer).setOnClickListener {
            val nouveau = lireParticipant() ?: return@setOnClickListener
            if (participants.any { memeIdentite(it, nouveau) }) {
                toast(R.string.erreur_doublon)
            } else {
                participants.add(nouveau)
                tvResultat.text = getString(R.string.inscription_reussie, nouveau.prenom, nouveau.nom)
                actualiserAffichage()
            }
        }
        findViewById<Button>(R.id.btnFiltrer).setOnClickListener {
            annulerSelection()
            actualiserAffichage()
        }
        lvParticipants.setOnItemClickListener { _, _, position, _ ->
            // La position appartient à la liste filtrée, pas à la collection complète.
            afficherSelection(participantsAffiches[position])
        }
        btnModifier.setOnClickListener { modifierSelection() }
        btnSupprimer.setOnClickListener { confirmerSuppression() }
        btnAnnuler.setOnClickListener { annulerSelection() }
        findViewById<Button>(R.id.btnOptions).setOnClickListener { ouvrirMenu(it as Button) }

        actualiserCompteur()
        actualiserBoutonsSelection()
    }

    private fun lireParticipant(): Participant? {
        val nom = etNom.text.toString().trim()
        val prenom = etPrenom.text.toString().trim()
        val age = etAge.text.toString().trim().toIntOrNull()
        val groupe = spGroupe.selectedItem?.toString().orEmpty()
        val support = when (rgSupport.checkedRadioButtonId) {
            R.id.rbEmulateur -> getString(R.string.radio_emulateur)
            R.id.rbTelephone -> getString(R.string.radio_telephone)
            else -> ""
        }

        val erreur = when {
            nom.isEmpty() || prenom.isEmpty() -> getString(R.string.erreur_nom_prenom)
            age == null || age !in 1..120 -> getString(R.string.erreur_age)
            groupe == getString(R.string.groupe_a_choisir) || groupe.isEmpty() -> getString(R.string.erreur_groupe)
            support.isEmpty() -> getString(R.string.erreur_support)
            !cbConfirmation.isChecked -> getString(R.string.erreur_confirmation)
            else -> null
        }
        if (erreur != null) {
            Toast.makeText(this, erreur, Toast.LENGTH_SHORT).show()
            return null
        }
        return Participant(nom, prenom, age!!, groupe, support)
    }

    private fun ouvrirMenu(ancre: Button) {
        val popup = PopupMenu(this, ancre)
        popup.menuInflater.inflate(R.menu.menu_atelier, popup.menu)
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.action_tous -> appliquerFiltre(0)
                R.id.action_g1 -> appliquerFiltre(1)
                R.id.action_g2 -> appliquerFiltre(2)
                R.id.action_g3 -> appliquerFiltre(3)
                R.id.action_apropos -> afficherAPropos()
                else -> return@setOnMenuItemClickListener false
            }
            true
        }
        popup.show()
    }

    private fun appliquerFiltre(position: Int) {
        annulerSelection()
        spFiltreGroupe.setSelection(position.coerceIn(0, 3))
        actualiserAffichage()
    }

    private fun afficherAPropos() {
        AlertDialog.Builder(this)
            .setTitle(R.string.menu_apropos)
            .setMessage(R.string.apropos_message)
            .setPositiveButton(R.string.fermer, null)
            .show()
    }

    private fun actualiserAffichage() {
        val filtre = spFiltreGroupe.selectedItemPosition
        val groupeChoisi = when (filtre) {
            1 -> "G1"
            2 -> "G2"
            3 -> "G3"
            else -> null
        }
        participantsAffiches.clear()
        participantsAffiches.addAll(
            if (groupeChoisi == null) participants
            else participants.filter { it.groupe == groupeChoisi }
        )
        lvParticipants.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            participantsAffiches.map { it.fiche() }
        )
        actualiserCompteur()
    }

    private fun afficherSelection(participant: Participant) {
        participantSelectionne = participant
        etNom.setText(participant.nom)
        etPrenom.setText(participant.prenom)
        etAge.setText(participant.age.toString())
        val positionGroupe = (0 until spGroupe.count).firstOrNull {
            spGroupe.getItemAtPosition(it).toString() == participant.groupe
        } ?: 0
        spGroupe.setSelection(positionGroupe)
        rgSupport.check(
            if (participant.support == getString(R.string.radio_telephone)) R.id.rbTelephone
            else R.id.rbEmulateur
        )
        cbConfirmation.isChecked = true
        tvResultat.text = getString(R.string.selection_active, participant.prenom, participant.nom)
        actualiserBoutonsSelection()
    }

    private fun modifierSelection() {
        val original = participantSelectionne ?: return toast(R.string.aucune_selection)
        val modifie = lireParticipant() ?: return
        val indexOriginal = participants.indexOfFirst { it === original }
        if (indexOriginal < 0) return annulerSelection()
        val doublon = participants.withIndex().any { (index, p) ->
            index != indexOriginal && memeIdentite(p, modifie)
        }
        if (doublon) return toast(R.string.erreur_doublon)
        participants[indexOriginal] = modifie
        participantSelectionne = modifie
        tvResultat.text = getString(R.string.modification_reussie)
        actualiserAffichage()
        actualiserBoutonsSelection()
    }

    private fun confirmerSuppression() {
        val cible = participantSelectionne ?: return toast(R.string.aucune_selection)
        AlertDialog.Builder(this)
            .setTitle(R.string.confirmation_suppression_titre)
            .setMessage(getString(R.string.confirmation_suppression_message, cible.prenom, cible.nom))
            .setNegativeButton(R.string.annuler, null)
            .setPositiveButton(R.string.supprimer) { _, _ ->
                participants.removeAll { it === cible }
                annulerSelection()
                actualiserAffichage()
            }
            .show()
    }

    private fun annulerSelection() {
        participantSelectionne = null
        reinitialiserFormulaire()
        tvResultat.text = getString(R.string.resultat_defaut)
        actualiserBoutonsSelection()
    }

    private fun reinitialiserFormulaire() {
        etNom.text.clear()
        etPrenom.text.clear()
        etAge.text.clear()
        spGroupe.setSelection(0)
        rgSupport.clearCheck()
        cbConfirmation.isChecked = false
    }

    private fun actualiserBoutonsSelection() {
        val actif = participantSelectionne != null
        btnModifier.isEnabled = actif
        btnSupprimer.isEnabled = actif
        btnAnnuler.isEnabled = actif
    }

    private fun memeIdentite(a: Participant, b: Participant): Boolean =
        a.nom.equals(b.nom, ignoreCase = true) && a.prenom.equals(b.prenom, ignoreCase = true)

    private fun actualiserCompteur() {
        tvCompteur.text = getString(R.string.compteur_inscriptions, participants.size)
    }

    private fun restaurerParticipants(etat: Bundle?) {
        val champs = etat?.getStringArrayList(CLE_PARTICIPANTS) ?: return
        if (champs.size % 5 != 0) return
        for (i in champs.indices step 5) {
            val nom = champs[i].trim()
            val prenom = champs[i + 1].trim()
            val age = champs[i + 2].toIntOrNull() ?: continue
            val groupe = champs[i + 3]
            val support = champs[i + 4]
            if (nom.isEmpty() || prenom.isEmpty() || age !in 1..120) continue
            if (groupe !in listOf("G1", "G2", "G3")) continue
            if (participants.any { it.nom.equals(nom, true) && it.prenom.equals(prenom, true) }) continue
            participants.add(Participant(nom, prenom, age, groupe, support))
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        val champs = ArrayList<String>(participants.size * 5)
        for (p in participants) {
            champs.add(p.nom)
            champs.add(p.prenom)
            champs.add(p.age.toString())
            champs.add(p.groupe)
            champs.add(p.support)
        }
        outState.putStringArrayList(CLE_PARTICIPANTS, champs)
        outState.putInt(CLE_FILTRE, spFiltreGroupe.selectedItemPosition)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        spFiltreGroupe.setSelection(savedInstanceState.getInt(CLE_FILTRE, 0).coerceIn(0, 3))
        actualiserAffichage()
    }

    override fun onStart() {
        super.onStart()
        Log.d(tagCycle, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(tagCycle, "onResume")
    }

    override fun onPause() {
        Log.d(tagCycle, "onPause")
        super.onPause()
    }

    override fun onStop() {
        Log.d(tagCycle, "onStop")
        super.onStop()
    }

    override fun onDestroy() {
        Log.d(tagCycle, "onDestroy")
        super.onDestroy()
    }

    private fun toast(message: Int) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {
        private const val CLE_PARTICIPANTS = "tn.tp.atelierandroid.PARTICIPANTS"
        private const val CLE_FILTRE = "tn.tp.atelierandroid.FILTRE"
    }
}

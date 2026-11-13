// PlanSécu — scripts communs
document.addEventListener('DOMContentLoaded', () => {
    initCreneaux();
    initCopie();
});

/** Formulaire de mission : ajout / retrait de créneaux, modèles d'horaires, total. */
function initCreneaux() {
    const form = document.getElementById('form-mission');
    const conteneur = document.getElementById('creneaux');
    const modele = document.getElementById('modele-creneau');
    if (!form || !conteneur || !modele) return;

    const total = document.getElementById('total-creneaux');

    const ajouter = (debut = '', fin = '') => {
        const ligne = modele.content.firstElementChild.cloneNode(true);
        ligne.querySelector('[data-champ="heureDebut"]').value = debut;
        ligne.querySelector('[data-champ="heureFin"]').value = fin;
        conteneur.appendChild(ligne);
        renumeroter();
        calculer();
        return ligne;
    };

    // Les noms "creneaux[i].heureDebut" doivent être continus pour le binding Spring
    const renumeroter = () => {
        conteneur.querySelectorAll('.creneau').forEach((ligne, i) => {
            ligne.querySelectorAll('[data-champ]').forEach(input => {
                input.name = `creneaux[${i}].${input.dataset.champ}`;
                input.id = `creneaux${i}.${input.dataset.champ}`;
            });
        });
    };

    const minutes = v => {
        if (!v) return null;
        const [h, m] = v.split(':').map(Number);
        return h * 60 + m;
    };

    const calculer = () => {
        let somme = 0;
        conteneur.querySelectorAll('.creneau').forEach(ligne => {
            const d = minutes(ligne.querySelector('[data-champ="heureDebut"]').value);
            const f = minutes(ligne.querySelector('[data-champ="heureFin"]').value);
            if (d === null || f === null || d === f) return;
            somme += f > d ? f - d : f + 1440 - d; // passage de minuit
        });
        if (total) total.textContent = `${Math.floor(somme / 60)}h${String(somme % 60).padStart(2, '0')}`;
    };

    document.getElementById('ajout-creneau')?.addEventListener('click', () => {
        ajouter().querySelector('input').focus();
    });

    conteneur.addEventListener('click', e => {
        const bouton = e.target.closest('.suppr-creneau');
        if (!bouton) return;
        bouton.closest('.creneau').remove();
        if (!conteneur.querySelector('.creneau')) ajouter();
        renumeroter();
        calculer();
    });

    conteneur.addEventListener('input', calculer);

    document.querySelectorAll('[data-modele]').forEach(b => b.addEventListener('click', () => {
        conteneur.innerHTML = '';
        b.dataset.modele.split(',').forEach(p => {
            const [d, f] = p.split('-');
            ajouter(d, f);
        });
    }));

    form.addEventListener('submit', renumeroter);
    renumeroter();
    calculer();
}

/** Boutons "copier" (lien d'abonnement calendrier). */
function initCopie() {
    document.querySelectorAll('[data-copier]').forEach(b => b.addEventListener('click', async () => {
        const cible = document.querySelector(b.dataset.copier);
        try {
            await navigator.clipboard.writeText(cible.value || cible.textContent);
            const texte = b.innerHTML;
            b.innerHTML = '<i class="bi bi-check-lg"></i> Copié';
            setTimeout(() => (b.innerHTML = texte), 1500);
        } catch {
            cible.select?.();
        }
    }));
}

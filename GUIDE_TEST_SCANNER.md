# Guide de Test du Scanner

## 📋 Prérequis

### 1. Installation des dépendances
```bash
cd InvoiceAI
npm install
```

### 2. Configuration Android/iOS

#### Android
- Assurez-vous d'avoir un émulateur Android ou un appareil physique connecté
- Les permissions caméra sont gérées automatiquement par l'app

#### iOS
- Nécessite un Mac avec Xcode
- Les permissions caméra sont gérées automatiquement par l'app

### 3. Base de données
- La base de données SQLite est créée automatiquement au premier lancement
- Aucune configuration supplémentaire nécessaire

---

## 🧪 Scénarios de Test

### Test 1 : Scanner une facture simple

**Objectif** : Vérifier que le flux complet fonctionne

**Étapes** :
1. Lancer l'application
2. Se connecter ou créer un compte
3. Aller dans l'onglet "Scanner"
4. Autoriser l'accès à la caméra si demandé
5. Pointer la caméra vers une facture/ticket
6. Appuyer sur le bouton de capture (cercle blanc au centre)
7. Attendre le traitement OCR (indicateur "Traitement de l'image...")
8. Vérifier l'écran de révision :
   - ✅ Marchand détecté
   - ✅ Date détectée
   - ✅ Total détecté
   - ✅ Articles détectés (si présents)
   - ✅ Catégories assignées automatiquement
   - ✅ Score de confiance affiché
9. Cliquer sur "Enregistrer"
10. Vérifier que la facture est sauvegardée

**Résultat attendu** :
- La facture est scannée, parsée et sauvegardée
- Navigation automatique après sauvegarde
- Facture visible dans la liste des factures

---

### Test 2 : Facture avec plusieurs articles

**Objectif** : Vérifier l'extraction de plusieurs items

**Étapes** :
1. Scanner une facture avec plusieurs articles (ex: ticket de supermarché)
2. Vérifier que tous les articles sont détectés
3. Vérifier que les montants sont corrects
4. Vérifier que les catégories sont assignées

**Résultat attendu** :
- Tous les articles sont listés
- Les montants sont corrects
- Les catégories sont pertinentes

---

### Test 3 : Facture avec faible qualité

**Objectif** : Tester la robustesse avec une image de mauvaise qualité

**Étapes** :
1. Scanner une facture floue ou mal éclairée
2. Observer le score de confiance
3. Vérifier ce qui est quand même détecté

**Résultat attendu** :
- Score de confiance plus bas (badge orange/rouge)
- Certaines informations peuvent manquer
- L'utilisateur peut quand même sauvegarder et éditer manuellement

---

### Test 4 : Mode Offline

**Objectif** : Vérifier que l'app fonctionne sans connexion

**Étapes** :
1. Désactiver le WiFi/Données mobiles
2. Scanner une facture
3. Sauvegarder
4. Vérifier que la facture est sauvegardée localement
5. Réactiver la connexion
6. Vérifier que la synchronisation se fait automatiquement

**Résultat attendu** :
- La facture est sauvegardée même sans connexion
- La synchronisation se fait automatiquement quand la connexion revient
- Le statut passe de "PENDING" à "SYNCED"

---

### Test 5 : Gestion des erreurs

**Objectif** : Vérifier la gestion des cas d'erreur

**Scénarios à tester** :
1. **Permission caméra refusée** :
   - Refuser l'accès à la caméra
   - Vérifier que l'écran de demande de permission s'affiche
   - Accepter et vérifier que ça fonctionne

2. **OCR échoue** :
   - Scanner une image sans texte (ex: photo de paysage)
   - Vérifier que l'app ne plante pas
   - Vérifier qu'un message d'erreur ou une facture vide est affichée

3. **Sauvegarde échoue** :
   - Simuler une erreur de base de données
   - Vérifier que l'utilisateur est informé

---

## 🔍 Points de Vérification

### Interface Utilisateur
- [ ] Le bouton de capture est visible et fonctionnel
- [ ] L'indicateur de traitement s'affiche pendant l'OCR
- [ ] L'écran de révision affiche toutes les informations
- [ ] Les badges de catégorie sont colorés correctement
- [ ] Le score de confiance est affiché avec la bonne couleur
- [ ] Les boutons "Annuler" et "Enregistrer" fonctionnent

### Fonctionnalités
- [ ] L'OCR extrait le texte correctement
- [ ] Le parsing détecte le marchand
- [ ] Le parsing détecte la date
- [ ] Le parsing détecte le total
- [ ] Le parsing extrait les articles
- [ ] La classification assigne les bonnes catégories
- [ ] La sauvegarde fonctionne
- [ ] La synchronisation fonctionne (si en ligne)

### Performance
- [ ] Le traitement OCR prend moins de 5 secondes
- [ ] L'interface reste réactive pendant le traitement
- [ ] Pas de lag lors de la navigation

---

## 🐛 Problèmes Connus / À Surveiller

### 1. Format de date
- Le parser détecte les dates au format DD/MM/YYYY ou DD-MM-YYYY
- Si la date n'est pas détectée, la date du jour est utilisée par défaut

### 2. Extraction d'articles
- Le parsing est basique et peut ne pas détecter tous les articles
- Les articles non détectés peuvent être ajoutés manuellement après sauvegarde

### 3. Classification
- La classification utilise uniquement des mots-clés pour l'instant
- Certaines catégories peuvent être incorrectes
- L'utilisateur peut corriger après sauvegarde

### 4. Synchronisation
- La synchronisation nécessite une connexion active
- Les factures sont sauvegardées localement même sans connexion
- La synchronisation se fait automatiquement quand la connexion revient

---

## 📝 Checklist de Test Rapide

Avant de tester, vérifiez :
- [ ] Les dépendances sont installées (`npm install`)
- [ ] L'émulateur/appareil est prêt
- [ ] Vous avez une facture/ticket à scanner
- [ ] La connexion internet est disponible (pour tester la sync)

Pendant le test :
- [ ] Notez les problèmes rencontrés
- [ ] Notez les temps de traitement
- [ ] Prenez des captures d'écran si nécessaire
- [ ] Testez avec différents types de factures

Après le test :
- [ ] Vérifiez que les factures sont dans la liste
- [ ] Vérifiez la base de données locale
- [ ] Vérifiez la synchronisation avec le backend (si configuré)

---

## 🚀 Commandes Utiles

### Lancer l'app
```bash
# Android
npm run android

# iOS
npm run ios
```

### Voir les logs
```bash
# Android
npx react-native log-android

# iOS
npx react-native log-ios
```

### Nettoyer et relancer
```bash
# Nettoyer le cache
npm start -- --reset-cache

# Android - Nettoyer le build
cd android && ./gradlew clean && cd ..
```

---

## 💡 Conseils de Test

1. **Commencez simple** : Testez d'abord avec une facture claire et simple
2. **Variez les formats** : Testez différents types de tickets (supermarché, restaurant, pharmacie)
3. **Testez les limites** : Essayez avec des factures floues, mal éclairées, ou avec beaucoup d'articles
4. **Vérifiez les données** : Après chaque scan, vérifiez que les données sont correctes
5. **Testez offline** : Assurez-vous que l'app fonctionne sans connexion

---

## 📞 Support

Si vous rencontrez des problèmes :
1. Vérifiez les logs de l'application
2. Vérifiez la console pour les erreurs JavaScript
3. Vérifiez que toutes les permissions sont accordées
4. Vérifiez que les dépendances natives sont bien installées

Bon test ! 🎉



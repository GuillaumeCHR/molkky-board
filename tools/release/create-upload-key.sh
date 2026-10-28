#!/usr/bin/env bash
# Crée la clé d'envoi Play Store de Molkky Board (upload key) et son fichier de propriétés, HORS du dépôt.
# Les mots de passe sont générés au hasard et ne sont jamais affichés. Voir docs/RELEASE.md pour la sauvegarde.
set -euo pipefail
DIR="${HOME}/.config/molkkyboard"
STORE="${DIR}/upload-key.jks"
PROPS="${DIR}/upload-key.properties"
if [[ -e "${STORE}" || -e "${PROPS}" ]]; then
  echo "Une clé existe déjà dans ${DIR} : rien n'est écrasé." >&2
  exit 1
fi
umask 077
mkdir -p "${DIR}"
PASS="$(head -c 32 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 32)"
keytool -genkeypair -v -keystore "${STORE}" -storetype PKCS12 -alias upload -keyalg RSA -keysize 4096 \
  -validity 10950 -storepass "${PASS}" -keypass "${PASS}" -dname "CN=Yogi Studio, O=Yogi Studio, C=FR" >/dev/null 2>&1
cat > "${PROPS}" <<PROPS
storeFile=${STORE}
storePassword=${PASS}
keyAlias=upload
keyPassword=${PASS}
PROPS
chmod 600 "${STORE}" "${PROPS}"
echo "Clé d'envoi créée : ${STORE}"
echo "Propriétés (mot de passe compris) : ${PROPS}"
echo "Empreinte SHA-256 du certificat (à garder, sans risque) :"
keytool -list -v -keystore "${STORE}" -storepass "${PASS}" -alias upload | grep -E "SHA256:" | sed 's/^\s*/  /'
echo "Sauvegardez MAINTENANT les deux fichiers dans votre gestionnaire de mots de passe (voir docs/RELEASE.md)."

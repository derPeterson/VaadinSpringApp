"""Lokaler Sonar-Zugang: Prozessvariable oder benutzergebunden verschlüsselte Datei."""

import ctypes
import os
from ctypes import wintypes
from pathlib import Path


class CredentialError(RuntimeError):
    pass


class DataBlob(ctypes.Structure):
    _fields_ = [("size", wintypes.DWORD), ("data", ctypes.POINTER(ctypes.c_ubyte))]


def credential_path():
    local_data = os.environ.get("LOCALAPPDATA")
    if not local_data:
        raise CredentialError("Lokale Windows-Zugangsablage ist nicht verfügbar.")
    return Path(local_data) / "VaadinSpringApp" / "Sonar" / "token.dpapi"


def protect_data(data, *, decrypt=False):
    """Windows DPAPI bindet den Zugang an den aktuellen Benutzer dieser Maschine."""
    if os.name != "nt":
        raise CredentialError("Außerhalb von Windows SONARQUBE_TOKEN verwenden.")
    buffer = ctypes.create_string_buffer(data)
    source = DataBlob(len(data), ctypes.cast(buffer, ctypes.POINTER(ctypes.c_ubyte)))
    target = DataBlob()
    crypt = ctypes.WinDLL("crypt32", use_last_error=True)
    kernel = ctypes.WinDLL("kernel32", use_last_error=True)
    kernel.LocalFree.argtypes = [ctypes.c_void_p]
    kernel.LocalFree.restype = ctypes.c_void_p
    operation = crypt.CryptUnprotectData if decrypt else crypt.CryptProtectData
    operation.argtypes = [
        ctypes.POINTER(DataBlob),
        ctypes.c_void_p,
        ctypes.c_void_p,
        ctypes.c_void_p,
        ctypes.c_void_p,
        wintypes.DWORD,
        ctypes.POINTER(DataBlob),
    ]
    operation.restype = wintypes.BOOL
    # CRYPTPROTECT_UI_FORBIDDEN: keine versteckten Rückfragen aus MCP-Prozessen.
    if not operation(
        ctypes.byref(source), None, None, None, None, 1, ctypes.byref(target)
    ):
        raise CredentialError(
            "Windows konnte den lokalen Sonar-Zugang nicht verarbeiten."
        )
    try:
        return ctypes.string_at(target.data, target.size)
    finally:
        kernel.LocalFree(target.data)


def load_token():
    token = os.environ.get("SONARQUBE_TOKEN", "").strip()
    if not token:
        path = credential_path()
        if not path.is_file():
            raise CredentialError(
                "Sonar-Zugang fehlt. Zuerst sonar.py token ausführen."
            )
        token = protect_data(path.read_bytes(), decrypt=True).decode("utf-8").strip()
    if not token or any(character.isspace() for character in token):
        raise CredentialError("Der lokale Sonar-Zugang ist ungültig.")
    return token


def store_token_dialog():
    """Nur der Mensch bedient dieses Fenster; Eingabe wird weder ausgegeben noch geloggt."""
    import tkinter as tk
    from tkinter import messagebox, simpledialog

    window = tk.Tk()
    window.withdraw()
    try:
        token = simpledialog.askstring(
            "SonarQube-Zugang für VaadinSpringApp",
            "SonarQube-BENUTZERTOKEN für http://localhost:9000 eingeben.\n"
            "Er wird lokal mit Windows DPAPI verschlüsselt gespeichert.\n"
            "Bitte keinen Projekt- oder globalen Analysetoken verwenden.",
            show="*",
            parent=window,
        )
        if token is None:
            raise CredentialError("Zugangseinrichtung abgebrochen; nichts gespeichert.")
        token = token.strip()
        if not token or any(character.isspace() for character in token):
            raise CredentialError("Kein gültiger Token eingegeben; nichts gespeichert.")
        encrypted = protect_data(token.encode("utf-8"))
        path = credential_path()
        path.parent.mkdir(parents=True, exist_ok=True)
        temporary = path.with_suffix(".tmp")
        temporary.write_bytes(encrypted)
        temporary.replace(path)
        messagebox.showinfo(
            "SonarQube", "Zugang lokal verschlüsselt gespeichert.", parent=window
        )
    finally:
        window.destroy()

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <libgen.h> // for basename on Linux, Windows has _splitpath

#ifdef _WIN32
#include <windows.h>
#endif

int main(int argc, char *argv[]) {
    if (argc < 2) {
        printf("Usage: dumper <filename>\n");
        return 1;
    }

    // Extract just the filename from the full path
    char *filename = argv[1];
#ifdef _WIN32
    char nameOnly[260];
    char extOnly[260];
    _splitpath(filename, NULL, NULL, nameOnly, extOnly);
    char shortName[520];
    snprintf(shortName, sizeof(shortName), "%s%s", nameOnly, extOnly);
#else
    char *shortName = basename(filename);
#endif

    // Output: save/filename_dump.txt
    char nomeSaida[520];
    snprintf(nomeSaida, sizeof(nomeSaida), "save\\%s_dump.txt", shortName);

    unsigned char byte;
    int contador = 0;

    FILE *entrada = fopen(argv[1], "rb");
    if (entrada == NULL) {
        printf("ERROR: Cannot open file '%s'\n", argv[1]);
        return 1;
    }

    FILE *saida = fopen(nomeSaida, "w");
    if (saida == NULL) {
        printf("ERROR: Cannot create '%s'\n", nomeSaida);
        fclose(entrada);
        return 1;
    }

    while (fread(&byte, 1, 1, entrada)) {
        fprintf(saida, "%02X ", byte);
        contador++;
        if (contador % 16 == 0) fprintf(saida, "\n");
    }

    printf("OK: %d bytes dumped to %s\n", contador, nomeSaida);

    fclose(entrada);
    fclose(saida);
    return 0;
}

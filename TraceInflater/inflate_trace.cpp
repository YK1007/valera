#include <stdio.h>
#include <assert.h>
#include "zlib.h"

#define CHUNK (300 * 1024)
#define TRACE_TAG_SEGMENT 1

int inf(FILE *source, FILE *dest)
{
    int ret;
    unsigned have;
    unsigned char in[CHUNK];
    unsigned char out[CHUNK];
    z_stream strm;


    /* decompress until deflate stream ends or end of file */
    do {
        int tag;
        fread(&tag, sizeof(int), 1, source);
        assert(tag == TRACE_TAG_SEGMENT);

        int size;
        fread(&size, sizeof(int), 1, source);
        assert(size >= 0 && size < CHUNK);

        /* allocate inflate state */
        strm.zalloc = Z_NULL;
        strm.zfree = Z_NULL;
        strm.opaque = Z_NULL;
        strm.avail_in = 0;
        strm.next_in = Z_NULL;
        ret = inflateInit(&strm);
        if (ret != Z_OK) {
            return ret;
        }
        strm.total_in = strm.avail_in = fread(in, 1, size, source);
        if (strm.avail_in != size || ferror(source)) {
            (void)inflateEnd(&strm);
            return Z_ERRNO;
        }
        if (strm.avail_in == 0)
            break;
        strm.next_in = in;

        strm.total_out = strm.avail_out = CHUNK;
        strm.next_out = out;
        ret = inflate(&strm, Z_FINISH);
        assert(ret == Z_STREAM_END);
        have = CHUNK - strm.avail_out;

        if (fwrite(out, 1, have, dest) != have || ferror(dest)) {
            (void)inflateEnd(&strm);
            return Z_ERRNO;
        }
    } while (!feof(source));

    /* clean up and return */
    (void)inflateEnd(&strm);
    return ret == Z_STREAM_END ? Z_OK : Z_DATA_ERROR;
}

/* report a zlib or i/o error */
void zerr(int ret)
{
    fputs("zpipe: ", stderr);
    switch (ret) {
    case Z_ERRNO:
        if (ferror(stdin))
            fputs("error reading stdin\n", stderr);
        if (ferror(stdout))
            fputs("error writing stdout\n", stderr);
        break;
    case Z_STREAM_ERROR:
        fputs("invalid compression level\n", stderr);
        break;
    case Z_DATA_ERROR:
        fputs("invalid or incomplete deflate data\n", stderr);
        break;
    case Z_MEM_ERROR:
        fputs("out of memory\n", stderr);
        break;
    case Z_VERSION_ERROR:
        fputs("zlib version mismatch!\n", stderr);
    }
}

int main(int argc, char **argv) {
    if (argc != 3) {
        fputs("usage: inflate_trace <trace_file_name> <output_file_name>\n", stderr);
        return 1;
    }

    FILE *source = fopen(argv[1], "rb");
//    FILE *dest = fopen(argv[2], "a");
    FILE *dest = fopen(argv[2], "w");
    int ret = inf(source, dest);
//    if (ret != Z_OK)
//        zerr(ret);

    fclose(source);
    fclose(dest);
    return ret;
}


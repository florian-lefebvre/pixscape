/*
 * Copyright (C) 2015 Laboratoire ThéMA - UMR 6049 - CNRS / Université de Franche-Comté
 * http://thema.univ-fcomte.fr
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

#include <stdio.h>
#include <cuda.h>
#include <stdlib.h>
#include <math.h>
#include <math_constants.h>

#define NCORE 512   

#define EARTH_DIAM 12740000

// TODO merge calcRay Bounds and Unbounded if the speed up is negligible

extern "C"
__global__ void calcRayDirect(int x0, int y0, float startZ, float destZ, float * dtm, int w, int h, 
        float res2D, int hasdsm, float * dsm, int earthCurv, float coefRefrac, unsigned char *view) {
    const int tid = blockIdx.x * blockDim.x + threadIdx.x;
    int x1, y1;
    if (tid < w) {
        x1 = tid;
        y1 = 0;
    } else if (tid < w + h) {
        x1 = w - 1;
        y1 = tid - w;
    } else if (tid < 2 * w + h) {
        x1 = tid - (w + h);
        y1 = h - 1;
    } else if (tid < 2 * w + 2 * h) {
        x1 = 0;
        y1 = tid - (2 * w + h);
    } else
        return;
    int ind = x0 + y0*w;
    const int ind1 = x1 + y1*w;
    const float z0 = dtm[ind] + startZ;
    const int dx = abs(x1 - x0);
    const int dy = abs(y1 - y0);
    const int sx = x0 < x1 ? 1 : -1;
    const int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    int xx = 0;
    int yy = 0;

    view[ind] = 1;
    
    float maxSlope = -1e127;
    float maxZ = -1e127;
    while (ind != ind1) {
        const int e2 = err * 2;
        if (e2 > -dy) {
            err -= dy;
            xx += sx;
            ind += sx;
        }
        if (e2 < dx) {
            err += dx;
            yy += sy;
            ind += sy*w;
        }
        
        float z = dtm[ind];    
        if(z == CUDART_NAN_F) {
            return;
        }
        
        const float d2 = res2D*res2D * (xx*xx + yy*yy);
        
        if(earthCurv) {
            z -= (1 - coefRefrac) * d2 / EARTH_DIAM;
        }
        
        const float zSurf = z + (hasdsm ? dsm[ind] : 0);
        const float zView = destZ == -1 ? zSurf : (z + destZ);
        
        if (maxSlope >= 0 && zSurf <= maxZ && zView <= maxZ) {
            continue;
        }
        
        const float zzSurf = (zSurf - z0);
        const float slopeSurf = zzSurf * fabs(zzSurf) / d2;
        if(zView >= zSurf) {
            if(zView == zSurf && slopeSurf > maxSlope) {
                view[ind] = 1;
            } else {
                const double zzView = (zView - z0);
                const double slopeView = zzView*fabs(zzView) / d2;
                if(slopeView > maxSlope)
                    view[ind] = 1;
            }
        }
        if(slopeSurf > maxSlope) {
            maxSlope = slopeSurf;
        }
        if(zSurf > maxZ) {
            maxZ = zSurf;
        }
 
    }
}

extern "C"
__global__ void calcRayIndirect(int x0, int y0, float startZ, float destZ, float * dtm, int w, int h, 
        float res2D, int hasdsm, float * dsm, int earthCurv, float coefRefrac, unsigned char *view) {
    const int tid = blockIdx.x * blockDim.x + threadIdx.x;
    int x1, y1;
    if (tid < w) {
        x1 = tid;
        y1 = 0;
    } else if (tid < w + h) {
        x1 = w - 1;
        y1 = tid - w;
    } else if (tid < 2 * w + h) {
        x1 = tid - (w + h);
        y1 = h - 1;
    } else if (tid < 2 * w + 2 * h) {
        x1 = 0;
        y1 = tid - (2 * w + h);
    } else
        return;
    int ind = x0 + y0*w;
    const int ind1 = x1 + y1*w;
    
    const float dsmZ = hasdsm ? dsm[ind] : 0;
    if(destZ != -1 && destZ < dsmZ)
        return;
    const float z0 = dtm[ind] + (destZ != -1 ? destZ : dsmZ);
    const int dx = abs(x1 - x0);
    const int dy = abs(y1 - y0);
    const int sx = x0 < x1 ? 1 : -1;
    const int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    int xx = 0;
    int yy = 0;

    view[ind] = 1;
    
    float maxSlope = -1e127;
    float maxZ = -1e127;
    while (ind != ind1) {
        const int e2 = err * 2;
        if (e2 > -dy) {
            err -= dy;
            xx += sx;
            ind += sx;
        }
        if (e2 < dx) {
            err += dx;
            yy += sy;
            ind += sy*w;
        }
        float z = dtm[ind];
        if(z == CUDART_NAN_F) {
            return;
        }
        const float dist = res2D * res2D * (xx * xx + yy * yy);
        if(earthCurv) {
            z -= (1 - coefRefrac) * dist / EARTH_DIAM;
        }
        if (maxSlope >= 0 && z + startZ <= maxZ) {
            continue;
        }
        float zz = (z + startZ - z0);
        float slope = zz * fabs(zz) / dist;
        if (slope > maxSlope) {
            view[ind] = 1;
        }
        const float ztot = z + (hasdsm ? dsm[ind] : 0);
        zz = ztot - z0;
        slope = zz * fabs(zz) / dist;
        if (slope > maxSlope)
            maxSlope = slope;
        if (ztot > maxZ)
            maxZ = ztot;
    }
}

extern "C"
__global__ void calcRayDirectBounded(int x0, int y0, float startZ, float destZ, float * dtm, int w, int h, 
            float res2D, int hasdsm, float * dsm, int earthCurv, float coefRefrac, unsigned char *view,
            // bounds
            float dMin2, float dMax2, float aleft, float aright, float sMin2, float sMax2) {
    const int tid = blockIdx.x * blockDim.x + threadIdx.x;
    int x1, y1;
    if (tid < w) {
        x1 = tid;
        y1 = 0;
    } else if (tid < w + h) {
        x1 = w - 1;
        y1 = tid - w;
    } else if (tid < 2 * w + h) {
        x1 = tid - (w + h);
        y1 = h - 1;
    } else if (tid < 2 * w + 2 * h) {
        x1 = 0;
        y1 = tid - (2 * w + h);
    } else
        return;
    
    double a = atan2((double)y0-y1, (double)x1-x0);
    if(a < 0)
        a += 2*M_PI;
    if(!(aright < aleft && a >= aright && a <= aleft || (aright >= aleft && (a >= aright || a <= aleft))))
        return;
        
    int ind = x0 + y0*w;
    const int ind1 = x1 + y1*w;
    float z0 = dtm[ind] + startZ;
    const int dx = abs(x1 - x0);
    const int dy = abs(y1 - y0);
    const int sx = x0 < x1 ? 1 : -1;
    const int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    int xx = 0;
    int yy = 0;

    if(sMin2 == -INFINITY && dMin2 == 0)
        view[ind] = 1;
    
    float maxSlope = sMin2;
    float maxZ = -1e127;
    while (ind != ind1) {
        const int e2 = err * 2;
        if (e2 > -dy) {
            err -= dy;
            xx += sx;
            ind += sx;
        }
        if (e2 < dx) {
            err += dx;
            yy += sy;
            ind += sy*w;
        }
        
        float z = dtm[ind];
        if(z == CUDART_NAN_F) {
            return;
        }
        const float d2 = (res2D*res2D * (xx * xx + yy * yy));
        if(d2 >= dMax2) {
            return;
        }
        
        if(earthCurv) {
            z -= (1 - coefRefrac) * d2 / EARTH_DIAM;
        }
        
        const float zSurf = z + (hasdsm ? dsm[ind] : 0);
        const float zView = destZ == -1 ? zSurf : (z + destZ);
            
        if (maxSlope >= 0 && zSurf <= maxZ && zView <= maxZ) {
            continue;
        }
        const float zzSurf = (zSurf - z0);
        const float slopeSurf = zzSurf * fabs(zzSurf) / d2;
        if(slopeSurf > sMax2)
            return;
        if(d2 >= dMin2 && zView >= zSurf) {
            if(zView == zSurf && slopeSurf > maxSlope) {
                view[ind] = 1;
            } else {
                const double zzView = (zView - z0);
                const double slopeView = zzView*fabs(zzView) / d2;
                if(slopeView > maxSlope)
                    view[ind] = 1;
            }
        }
        
        if(slopeSurf > maxSlope) {
            maxSlope = slopeSurf;
        }
        if(zSurf > maxZ) {
            maxZ = zSurf;
        }
    }
}

extern "C"
__global__ void calcRayIndirectBounded(int x0, int y0, float startZ, float destZ, float * dtm, int w, int h, 
            float res2D, int hasdsm, float * dsm, int earthCurv, float coefRefrac, unsigned char *view,
            float dMin2, float dMax2, float aleft, float aright, float sMin2, float sMax2) {
    const int tid = blockIdx.x * blockDim.x + threadIdx.x;
    int x1, y1;
    if (tid < w) {
        x1 = tid;
        y1 = 0;
    } else if (tid < w + h) {
        x1 = w - 1;
        y1 = tid - w;
    } else if (tid < 2 * w + h) {
        x1 = tid - (w + h);
        y1 = h - 1;
    } else if (tid < 2 * w + 2 * h) {
        x1 = 0;
        y1 = tid - (2 * w + h);
    } else {
        return;
    }
    
    double a = atan2((double)y0-y1, (double)x1-x0);
    if(a < 0) {
        a += 2*M_PI;
    }
    if(!(aright < aleft && a >= aright && a <= aleft || (aright >= aleft && (a >= aright || a <= aleft)))) {
        return;
    }
    int ind = x0 + y0*w;
    const int ind1 = x1 + y1*w;
    
    const float dsmZ = hasdsm ? dsm[ind] : 0;
    if(destZ != -1 && destZ < dsmZ) {
        return;
    }
    const float z0 = dtm[ind] + (destZ != -1 ? destZ : dsmZ);
    const int dx = abs(x1 - x0);
    const int dy = abs(y1 - y0);
    const int sx = x0 < x1 ? 1 : -1;
    const int sy = y0 < y1 ? 1 : -1;
    int err = dx - dy;
    int xx = 0;
    int yy = 0;

    if(sMin2 == -INFINITY && dMin2 == 0) {
        view[ind] = 1;
    }
    
    float maxSlope = sMin2;
    float maxZ = -1e127;
    while (ind != ind1) {
        const int e2 = err * 2;
        if (e2 > -dy) {
            err -= dy;
            xx += sx;
            ind += sx;
        }
        if (e2 < dx) {
            err += dx;
            yy += sy;
            ind += sy*w;
        }
        float z = dtm[ind];
        if(z == CUDART_NAN_F) {
            return;
        }
        const float d2 = res2D*res2D * (xx * xx + yy * yy);
        if(d2 >= dMax2) {
            return;
        }
        
        if(earthCurv) {
            z -= (1 - coefRefrac) * d2 / EARTH_DIAM;
        }
        if (maxSlope >= 0 && z + startZ <= maxZ) {
            continue;
        }
        
        float zz = (z + startZ - z0);
        const float slopeEye = zz * fabs(zz) / d2;
        if (slopeEye > maxSlope) {
            if(d2 >= dMin2 && slopeEye <= sMax2)
                view[ind] = 1;
        }
        const float ztot = z + (hasdsm ? dsm[ind] : 0);
        zz = ztot - z0;
        const float slope = zz * fabs(zz) / d2;
        if (slope > maxSlope) {
            maxSlope = slope;
        }
        if(maxSlope > sMax2) {
            return;
        }
        if (ztot > maxZ) {
            maxZ = ztot;
        }
    }
}

extern "C"
__global__  void calcRayTan(int x0, int y0, double startZ, float * dtm, int w, int h, 
            double res2D, int hasdsm, float * dsm, int earthCurv, float coefRefrac, int *view, int wa, double ares,
            double dMin, double dMax, double aleft, double aright, double sMin, double sMax) {
    
    const int ax = blockIdx.x * blockDim.x + threadIdx.x;
    if (ax >= wa)
        return;
    double a = (aleft - ax*ares);
    if(a < 0)
        a += 2*M_PI;

    if(!(aright < aleft && a >= aright && a <= aleft || (aright >= aleft && (a >= aright || a <= aleft))))
        return;

    int y1 = a >= 0 && a < M_PI ? 0 : h-1; // haut ou bas ?
    int x1 = a >= M_PI/2 && a < 1.5*M_PI ? 0 : w-1; // droite ou gauche ?
    int sens = x1 == 0 ? -1 : +1;
    
    int ddy = -round(tan(a) * abs(x1-x0));
    int y = y0 + sens * ddy;   
    if(y >= 0 && y < h) {
        y1 = y;   
    } else {
        int ddx = abs(round(tan(M_PI/2+a) * abs(y1-y0)));
        x1 = x0 + sens * ddx;
    }
    
/*
    if(a == 0) { // droite
        y1 = y0;
        x1 = w-1;
    } else if(a < M_PI/2) { // haut droit
        int dx = round(tan(M_PI/2-a) * y0); 
        if(x0+dx < w) {
            x1 = x0 + dx;
            y1 = 0;
        } else {
            int dy = round(tan(a) * (w-1-x0)); 
            x1 = w-1;
            y1 = y0-dy;
        }
    } else if(a == M_PI/2) { // haut 
        y1 = 0;
        x1 = x0;
    } else if(a < M_PI) { // haut gauche
        int dx = round(tan(a-M_PI/2) * y0); 
        if(x0-dx >= 0) {
            x1 = x0 - dx;
            y1 = 0;
        } else {
            int dy = round(tan(M_PI-a) * x0); 
            x1 = 0;
            y1 = y0-dy;
        }
    } else if(a == M_PI) { // gauche
        x1 = 0;
        y1 = y0;
    } else if(a < 1.5*M_PI) { // bas gauche
        int dx = round(tan(1.5*M_PI-a) * (h-1-y0)); 
        if(x0-dx >= 0) {
            x1 = x0 - dx;
            y1 = h-1;
        } else {
            int dy = round(tan(a-M_PI) * x0); 
            x1 = 0;
            y1 = y0+dy;
        }
    } else if(a == 1.5*M_PI) { // bas
        x1 = x0;
        y1 = h-1;
    } else { // bas droit
        int dx = round(tan(a-1.5*M_PI) * (h-1-y0)); 
        if(x0+dx >= 0 && x0+dx < w) {
            x1 = x0 + dx;
            y1 = h-1;
        } else {
            int dy = round(tan(2*M_PI-a) * (w-1-x0)); 
            x1 = w-1;
            y1 = y0+dy;
        }
    }
*/
    
    if(x1 < 0 || x1 >= w || y1 < 0 || y1 >= h) {
        for(int yz = 0; yz < wa/2; yz++) {
            view[yz*wa + ax] = -1;
        }
        return;
    }
    
    const int dx = abs(x1-x0);
    const int dy = abs(y1-y0);
    const int sx = x0 < x1 ? 1 : -1;
    const int sy = y0 < y1 ? 1 : -1;
    int err = dx-dy;
    int xx = 0;
    int yy = 0;
    int ind = x0 + y0*w;
    const int ind1 = x1 + y1*w;
    const double z0 = dtm[ind] + startZ;
    
    if(dMin == 0) {
        const double si = min(-startZ / (res2D/2), sMax);
        const int zi1 = (int) ((M_PI/2 - atan(si)) / ares);
        const int zi2 = (int) ((M_PI/2 - atan(sMin)) / ares);
        for(int yz = zi1; yz < zi2; yz++) {
            view[yz*wa + ax] = ind;
        }
    }
    double maxSlope = max(-startZ / (res2D/2), sMin);
    double maxZ = -1e127;
    while(ind != ind1) {
        const int e2 = err << 1;
        if(e2 > -dy) {
            err -= dy;
            xx += sx;
            ind += sx;
        }
        if(e2 < dx) {
            err += dx;
            yy += sy;
            ind += sy*w;
        }
        double z = dtm[ind] + (hasdsm ? dsm[ind] : 0);
        if(z == CUDART_NAN) {
            return;
        }
        if(maxSlope >= 0 && z <= maxZ) {
            continue;
        }
        const double dist = res2D * sqrt((double)(xx*xx + yy*yy)) - copysign(1.0, z-z0)*res2D/2;
        if(dist > dMax) {
            return;
        }
        if(earthCurv) {
            z -= (1 - coefRefrac) * dist*dist / EARTH_DIAM;
        }
        const double slope = (z - z0) / dist;
        if(slope > maxSlope) {
            if(dist >= dMin) {
                const double s2 = min(sMax, slope);
                // tester Math.round à la place de ceil
                const int z2 = (int) round((M_PI/2 - atan(maxSlope)) / ares);
                const int z1 = (int) ((M_PI/2 - atan(s2)) / ares);

                for(int yz = z1; yz < z2; yz++) {
                    const int i = yz*wa + ax;
                    if(view[i] == -1) {
                        view[i] = (int) ind;
                    }
                }
            }   
            maxSlope = slope;
        }
        if(maxSlope > sMax) {
            return;
        }
        if(z > maxZ) {
            maxZ = z;
        }
    }

}

extern "C"
__global__ void sumView(unsigned char *g_idata, unsigned int n, int *g_odata) {
__shared__ int sdata[NCORE];
unsigned int tid = threadIdx.x;
unsigned int i = blockIdx.x*NCORE + tid;
unsigned int gridSize = NCORE*gridDim.x;
sdata[tid] = 0;

while (i < n) { sdata[tid] += g_idata[i]; i += gridSize; }
__syncthreads();
if (NCORE >= 512) { if (tid < 256) { sdata[tid] += sdata[tid + 256]; } __syncthreads(); }
if (NCORE >= 256) { if (tid < 128) { sdata[tid] += sdata[tid + 128]; } __syncthreads(); }
if (NCORE >= 128) { if (tid < 64) { sdata[tid] += sdata[tid + 64]; } __syncthreads(); }
if (tid < 32) {
    sdata[tid] += sdata[tid + 32];
     __syncthreads();
    sdata[tid] += sdata[tid + 16];
     __syncthreads();
    sdata[tid] += sdata[tid + 8];
     __syncthreads();
    sdata[tid] += sdata[tid + 4];
     __syncthreads();
    sdata[tid] += sdata[tid + 2];
     __syncthreads();
    sdata[tid] += sdata[tid + 1];
    }
if (tid == 0) 
    g_odata[blockIdx.x] = sdata[0];
}

extern "C"
__global__ void sumViewTan(int *g_idata, unsigned int n, int *g_odata) {
__shared__ int sdata[NCORE];
unsigned int tid = threadIdx.x;
unsigned int i = blockIdx.x*NCORE + tid;
unsigned int gridSize = NCORE*gridDim.x;
sdata[tid] = 0;

while (i < n) { 
    if(g_idata[i] > -1)
        sdata[tid]++; 
    i += gridSize; 
}
__syncthreads();
if (NCORE >= 512) { if (tid < 256) { sdata[tid] += sdata[tid + 256]; } __syncthreads(); }
if (NCORE >= 256) { if (tid < 128) { sdata[tid] += sdata[tid + 128]; } __syncthreads(); }
if (NCORE >= 128) { if (tid < 64) { sdata[tid] += sdata[tid + 64]; } __syncthreads(); }
if (tid < 32) {
    sdata[tid] += sdata[tid + 32];
     __syncthreads();
    sdata[tid] += sdata[tid + 16];
     __syncthreads();
    sdata[tid] += sdata[tid + 8];
     __syncthreads();
    sdata[tid] += sdata[tid + 4];
     __syncthreads();
    sdata[tid] += sdata[tid + 2];
     __syncthreads();
    sdata[tid] += sdata[tid + 1];
    }
if (tid == 0) 
    g_odata[blockIdx.x] = sdata[0];
}

extern "C"
__global__ void sumLandView(unsigned char * g_idata, unsigned int n, unsigned char * land, unsigned char code, int * g_odata) {
__shared__ int sdata[NCORE];
unsigned int tid = threadIdx.x;
unsigned int i = blockIdx.x*NCORE + tid;
unsigned int gridSize = NCORE*gridDim.x;
sdata[tid] = 0;

while (i < n) { 
    if(land[i] == code)
        sdata[tid] += g_idata[i]; 
    i += gridSize; 
}
__syncthreads();
if (NCORE >= 512) { if (tid < 256) { sdata[tid] += sdata[tid + 256]; } __syncthreads(); }
if (NCORE >= 256) { if (tid < 128) { sdata[tid] += sdata[tid + 128]; } __syncthreads(); }
if (NCORE >= 128) { if (tid < 64) { sdata[tid] += sdata[tid + 64]; } __syncthreads(); }
if (tid < 32) {
    sdata[tid] += sdata[tid + 32];
     __syncthreads();
    sdata[tid] += sdata[tid + 16];
     __syncthreads();
    sdata[tid] += sdata[tid + 8];
     __syncthreads();
    sdata[tid] += sdata[tid + 4];
     __syncthreads();
    sdata[tid] += sdata[tid + 2];
     __syncthreads();
    sdata[tid] += sdata[tid + 1];
    }
if (tid == 0) 
    g_odata[blockIdx.x] = sdata[0];
}

extern "C"
__global__ void sumLandViewTan(int * g_idata, unsigned int n, unsigned char * land, unsigned char code, int * g_odata) {
__shared__ int sdata[NCORE];
unsigned int tid = threadIdx.x;
unsigned int i = blockIdx.x*NCORE + tid;
unsigned int gridSize = NCORE*gridDim.x;
sdata[tid] = 0;

while (i < n) { 
    if(g_idata[i] > -1 && land[g_idata[i]] == code)
        sdata[tid]++; 
    i += gridSize; 
}
__syncthreads();
if (NCORE >= 512) { if (tid < 256) { sdata[tid] += sdata[tid + 256]; } __syncthreads(); }
if (NCORE >= 256) { if (tid < 128) { sdata[tid] += sdata[tid + 128]; } __syncthreads(); }
if (NCORE >= 128) { if (tid < 64) { sdata[tid] += sdata[tid + 64]; } __syncthreads(); }
if (tid < 32) {
    sdata[tid] += sdata[tid + 32];
     __syncthreads();
    sdata[tid] += sdata[tid + 16];
     __syncthreads();
    sdata[tid] += sdata[tid + 8];
     __syncthreads();
    sdata[tid] += sdata[tid + 4];
     __syncthreads();
    sdata[tid] += sdata[tid + 2];
     __syncthreads();
    sdata[tid] += sdata[tid + 1];
    }
if (tid == 0) 
    g_odata[blockIdx.x] = sdata[0];
}


extern "C"
__global__ void clearView(unsigned char *view, int size) {
    int tid = (blockIdx.x + gridDim.x * blockIdx.y) * blockDim.x + threadIdx.x;
    if (tid < size)
        view[tid] = 0;
}

extern "C"
__global__ void clearViewTan(int *view, int size) {
    int tid = (blockIdx.x + gridDim.x * blockIdx.y) * blockDim.x + threadIdx.x;
    if (tid < size)
        view[tid] = -1;
}

extern "C"
__global__ void addView(unsigned char *view, int * addView, int size) {
    int tid = (blockIdx.x + gridDim.x * blockIdx.y) * blockDim.x + threadIdx.x;
    if (tid < size)
        addView[tid] += view[tid];
}

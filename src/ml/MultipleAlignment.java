/*
 * Copyright (C) 2009 Simon A. Berger
 * 
 *  This program is free software; you may redistribute it and/or modify its
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 */

package ml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import javax.management.RuntimeErrorException;

/**
 *
 * @author sim
 */
public class MultipleAlignment implements Serializable {
    int nTaxon;
    int seqLen;
    String[] names;
    String[] data;
    HashMap<String,Integer> nameMap;

    
    
    public static boolean USE_SHITTY_LOADER = false;
    
    public MultipleAlignment(int nTaxon, int seqLen) {
        this(nTaxon, seqLen, nTaxon);
    }

    public MultipleAlignment(int nTaxon, int seqLen, int arraysize) {
        this.nTaxon = nTaxon;
        this.seqLen = seqLen;
        this.names = new String[arraysize];
        this.data = new String[arraysize];
    }
    
    public MultipleAlignment( int seqLen, String[] names, String[] seqs) {
		this.seqLen = seqLen;
		this.nTaxon = names.length;
		this.names = names;
		this.data = seqs;
    	
	}

	public static MultipleAlignment loadPhylip( File file ) {
    	if( USE_SHITTY_LOADER ) {
    		return loadPhylipShitty(file);
    	} else {
    		try {
				return loadPhylip(new FileReader(file));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new RuntimeException( "bailing out" );
			}
    	}
    	
    	
    }
    
    public static MultipleAlignment loadPhylip( InputStream is ) {
    	return loadPhylip(new InputStreamReader(is));
    }
    
    public static MultipleAlignment loadPhylip( Reader rarg ) {
        try {
            BufferedReader r = new BufferedReader(rarg);

            String header = r.readLine();
            StringTokenizer st = new StringTokenizer(header);

            int nTaxon = Integer.parseInt(st.nextToken());
            int seqLen = Integer.parseInt(st.nextToken());

            MultipleAlignment ma = new MultipleAlignment(nTaxon, seqLen);

            for( int i = 0; i < nTaxon; i++ ) {
                String line = r.readLine();
                if( line == null ) {
                    throw new RuntimeException( "cannot read next line in " + "input file" );
                }

                st = new StringTokenizer(line);
                String name = st.nextToken();
                String data = st.nextToken();

                if( data.length() != seqLen ) {
                    throw new RuntimeException("wrong sequence length: " + data.length() + " vs " + seqLen );
                }
                ma.names[i] = name;
                ma.data[i] = data.toUpperCase();
                
            }

            ma.buildNameIndex();

            r.close();
            return ma;

        } catch (IOException ex) {
            Logger.getLogger(MultipleAlignment.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("bailing out");
        }


    }

//    public static MultipleAlignment loadPhylipShitty( File file ) {
//        try {
//        	
//        	
//            BufferedReader r = new BufferedReader(new FileReader(file));
//
//            String header = r.readLine();
//            StringTokenizer st = new StringTokenizer(header);
//
//            final int nTaxon = Integer.parseInt(st.nextToken());
//            final int seqLen = Integer.parseInt(st.nextToken());
//
//            MultipleAlignment ma = new MultipleAlignment(nTaxon, seqLen);
//
//            ma.nameMap = new HashMap<String, Integer>();
//            
//            
//            int clines = 0;
//            int ctaxon = 0;
//            String line;
//            
//            while( (line = r.readLine()) != null ) 
//            {
//                
//                st = new StringTokenizer(line);
//                
//                int ptr = 0;
//                while( !Character.isWhitespace(line.charAt( ptr ))) {
//                	ptr++;
//                }
//                
//                if( ptr == 0 || ptr >= line.length() ) {
//                	throw new RuntimeException( "error: could not parse taxon name in line " + clines + "(ptr: " + ptr + ", " + line.length() + ")");
//                }
//                
//                String name = line.substring(0, ptr );
//                String data = line.substring(ptr);
//
//                data.replaceAll("\\s", "");
//                
//                if( ma.nameMap.containsKey(name)) {
//                	int i = ma.nameMap.get(name);
//                	
//                	assert( name.equals(ma.names[i]));
//                	ma.data[i] += data;
//                	
//                } else {
//                	if( ctaxon >= nTaxon ) {
//                		throw new RuntimeException( "more different taxon names in data section than in header");
//                	}
//                	
//                	ma.nameMap.put(name, ctaxon);
//                	ma.names[ctaxon] = name;
//                	ma.data[ctaxon] = data;
//                	ctaxon++;
//                	
//                	
//                }
//                
//                clines++;
//            }
//
//            if( ctaxon != nTaxon ) {
//            	throw new RuntimeException( "less different taxon names in data section than in header");
//            }
//
//            // sanity check on sequence length
//            for(int i = 0; i < nTaxon; i++ ) {
//            	if( ma.nameMap.get(ma.names[i]) != i ) {
//            		throw new RuntimeException( "quirk in name index");
//            	}
//            	
//            	if( ma.data.length != seqLen ) {
//            		throw new RuntimeException( "wrong sequence length");
//            	}
//            	
//            }
//            
//            r.close();
//            return ma;
//
//        } catch (IOException ex) {
//            Logger.getLogger(MultipleAlignment.class.getName()).log(Level.SEVERE, null, ex);
//            throw new RuntimeException("bailing out");
//        }
//
//
//    }

    
    private static String[] readLine( String line ) {
        int ptr = 0;
        
   //     System.out.printf( "readLine: '%s'\n", line );
        while( !Character.isWhitespace(line.charAt( ptr ))) {
        	ptr++;
        }
        
        String name;
        if( ptr == 0 ) {
        	name = null;
        } else {
        	name = line.substring(0, ptr);
        }

        
        String data = line.substring(ptr);
        //data = data.replaceAll( "\\s", "" );
        
        int cAlignChars = 0;
        for( int i = 0; i < data.length(); i++ ) {
        	if( isAlignmentChar(data.charAt(i))) {
        		cAlignChars++;
        	}
        }
        
        if( cAlignChars != data.length()) 
        {
//        	if( true ) {
//        		System.out.printf( "%d %d\n", cAlignChars, data.length() );
//        		throw new RuntimeException( "uuhm" );
//        	}
        	
        	char[] buf = new char[cAlignChars];
        	int bptr = 0;
        	for( int i = 0; i < data.length(); i++ ) {
        		char ch = data.charAt(i);
        		if( isAlignmentChar(ch)) {
        			buf[bptr++] = ch;
        		}
        	}
        	
        	data = new String(buf);
        }
        
        
//        for( int i = 0; i < data.length(); i++ ) {
//        	char ch = data.charAt(i);
//        	
//        	if( !isAlignmentChar(ch)) {
//        		System.out.printf( "%s\n", data );
//        		throw new RuntimeException( "bad character in sequence: " + (int)ch );
//        	}
//        }
        if( data.length() == 0 ) {
        	throw new RuntimeException( "sanity check: readLine called on empty line");
        }
        
        String[] ret = {name, data};
        return ret;
    }
    
    private static boolean isAlignmentChar(char ch) {
    	return Character.isLetter(ch) || ch == '?' || ch == '-';
    }

	public static MultipleAlignment loadPhylipShitty( File file ) {
        try {
        	
        	
            BufferedReader r = new BufferedReader(new FileReader(file));

            String header = r.readLine();
            StringTokenizer st = new StringTokenizer(header);

            final int nTaxon = Integer.parseInt(st.nextToken());
            final int seqLen = Integer.parseInt(st.nextToken());

            MultipleAlignment ma = new MultipleAlignment(nTaxon, seqLen);

            ma.nameMap = new HashMap<String, Integer>();
            
            String line = null;
            boolean keepline = false;
            
            mainloop:
            while( true ) {
	            for( int i = 0; i < nTaxon; i++ ) {
	            	if( !keepline ) {
	            		line = r.readLine();
	            	}
	            	keepline = false;
	            	
	            	if( line == null ) {
	            		break mainloop;
	            	}
	            	
	            	String nd[] = readLine(line); 
	            	
	            	String name = nd[0];
	            	String data = nd[1];
	            	
	            	if( name != null ) {
	            		assert( !ma.nameMap.containsKey(name));
	            		
	            		ma.names[i] = name;
	            		ma.data[i] = data;
	            		ma.nameMap.put( name, i );
	            		
	            	} else {
	            		ma.data[i] += data;
	            	}
	            }
	            
	            //
	            // look if there are non-empty lines following the last block.
	            // keepline is used to implement a crude form of 'unreadline'.
	            //
	            keepline = false;
	            outer:
	            while( (line = r.readLine()) != null ) {
	            	//System.out.printf( "look: '%s'\n", line );	            	
	            	for( int i = 0; i < line.length(); i++ ) {
	            		if( !Character.isWhitespace(line.charAt(i))) {
	            			keepline = true;
	            			break outer;
	            		}
	            	}
	            }
	            
	            if( !keepline ) {
	            	break;
	            }
	             
            }

            // sanity check on sequence length
            for(int i = 0; i < nTaxon; i++ ) {
            	if( ma.nameMap.get(ma.names[i]) != i ) {
            		throw new RuntimeException( "quirk in name index");
            	}
            	
            	if( ma.data[i].length() != seqLen ) {
            		throw new RuntimeException( "wrong sequence length " );
            	}
            	
            }
            
            r.close();
            return ma;

        } catch (IOException ex) {
            Logger.getLogger(MultipleAlignment.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("bailing out");
        }


    }

    
    public static String padSpaceRight( String s, int len ) {
        if( s.length() >= len ) {
            return s;
        } else {
            while( s.length() < len ) {
                s += " ";
            }
            return s;
        }
    }

    
    public void writePhylip( PrintStream w ) {
    	w.printf( "%d %d\n", nTaxon, seqLen);

        int maxNameLen = 0;

        for( int i = 0; i < nTaxon; i++ ) {
            maxNameLen = Math.max( maxNameLen, names[i].length());
        }

        for( int i = 0; i < nTaxon; i++ ) {
            w.printf( "%s%s\n", padSpaceRight(names[i], maxNameLen + 2), data[i]);
        }	
    }
    
    public void writePhylip( File file ) {
        try {
        	
        	
            PrintStream w;
            
            // transparent gz compression
            if( file.getName().endsWith(".gz")) {
            	w = new PrintStream(new GZIPOutputStream( new FileOutputStream(file)));
            } else {
            	w = new PrintStream(new FileOutputStream(file));
            }

            writePhylip(w);
            w.close();
        } catch (IOException ex) {
            Logger.getLogger(MultipleAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void writeFuckholm( File file ) {
        try {
        	
        	
            PrintStream w;
            
            // transparent gz compression
            if( file.getName().endsWith(".gz")) {
            	w = new PrintStream(new GZIPOutputStream( new FileOutputStream(file)));
            } else {
            	w = new PrintStream(new FileOutputStream(file));
            }

            writeFuckholm(w);
            w.close();
        } catch (IOException ex) {
            Logger.getLogger(MultipleAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void writeFuckholm(PrintStream w) {
    	
    	w.println( "# STOCKHOLM 1.0" );

        int maxNameLen = 0;

        for( int i = 0; i < nTaxon; i++ ) {
            maxNameLen = Math.max( maxNameLen, names[i].length());
        }

        for( int i = 0; i < nTaxon; i++ ) {
            w.printf( "%s%s\n", padSpaceRight(names[i], maxNameLen + 2), data[i]);
        }
        
        w.println( "//\n" );
	}

    
    public void writeFastaNogaps( File file ) {
        try {
        	
        	
            PrintStream w;
            
            // transparent gz compression
            if( file.getName().endsWith(".gz")) {
            	w = new PrintStream(new GZIPOutputStream( new FileOutputStream(file)));
            } else {
            	w = new PrintStream(new FileOutputStream(file));
            }

            writeFastaNogaps(w);
            w.close();
        } catch (IOException ex) {
            Logger.getLogger(MultipleAlignment.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private void writeFastaNogaps(PrintStream w) {
    	
    	

        
        for( int i = 0; i < nTaxon; i++ ) {
            w.printf( ">%s\n", names[i] );
            
            for( char c : data[i].toCharArray() ) {
            	if( c != '-' ) {
            		w.print(c);
            	}
            }
            w.println();
        }
        
        
	}

    
	private void buildNameIndex() {
        nameMap = new HashMap<String, Integer>();
        for( int i = 0; i < nTaxon; i++ ) {
            if( names[i] == null ) {
                throw new RuntimeException("cannot build name map: name is null: " + i );
            }
            nameMap.put( names[i], i );
        }
    }

    public void print() {
        for( int i = 0; i < names.length; i++ ) {
            System.out.printf( "'%s': '%s'\n", names[i], data[i] );
        }
    }

    public int nameToIdx( String name ) {
        if( nameMap.containsKey(name)) {
            return nameMap.get(name);
        } else {
            return -1;
        }
    }

    String getSequence( String name ) {
        int idx = nameToIdx(name);

        if( idx < 0 ) {
            throw new RuntimeException("taxon name not found: " + name );
        }
        return getSequence(idx);
    }

    String getSequence( int i ) {
        return data[i];
    }

    void replaceSequence( String name, String seq ) {
        int idx = nameToIdx(name);
        if( idx < 0 ) {
            throw new RuntimeException("taxon name not found: " + name );
        }

        data[idx] = seq;

    }

    public static void main( String args[] ) {
    	
    	if( false ) {
	        //MultipleAlignment ma = MultipleAlignment.loadPhylipShitty(new File( "/space/raxml/VINCENT/DATA/500"));
	    	long time1 = System.currentTimeMillis();
	    	MultipleAlignment mar = MultipleAlignment.loadPhylip(new File( "/space/raxml/VINCENT/DATA/150"));
	    	
	    	MultipleAlignment ma = MultipleAlignment.loadPhylipShitty(new File( "/space/raxml/VINCENT/DATA/150"));
	    	System.out.printf( "parse done: %d %s\n", System.currentTimeMillis() - time1, compare( ma, mar ) );
	
	    	//   ma.print();
    	} else {
    		long time1 = System.currentTimeMillis();
    		MultipleAlignment ma = MultipleAlignment.loadPhylipShitty(new File( "/space/raxml/VINCENT/DATA/500"));
    		System.out.printf( "parse done: %d\n", System.currentTimeMillis() - time1 );
    	}
    }

	private static boolean compare(MultipleAlignment ma, MultipleAlignment mar) {
		if( ma.names.length != mar.names.length ) {
			System.out.printf( "1\n" );
			return false;
		}
		
		if( ma.data.length != mar.data.length ) {
			System.out.printf( "2\n" );
			return false;
		}
		
		
		for( int i = 0; i < ma.data.length; i++ ) {
			if( ma.seqLen != ma.data[i].length() ) {
				System.out.printf( "3\n" );
				return false;
			}
			
			if( !ma.names[i].equals(mar.names[i])) {
				System.out.printf( "4\n" );
				return false;
			}
			
			if( !ma.data[i].equals(mar.data[i])) {
				System.out.printf( "5\n" );
				return false;
			}
			
			if( !ma.nameMap.get(ma.names[i]).equals(mar.nameMap.get( mar.names[i]))) {
				System.out.printf( "6\n" );
				return false;
			}
		}
		
		return true;
	}

	public int append( String name, String seq ) {
		if( nTaxon >= names.length ) {
			throw new RuntimeException( "cannot append more sequences");
		}
		names[nTaxon] = name;
		data[nTaxon] = seq;
		
		nTaxon++;
		return nTaxon-1;
	}
	
	public MultipleAlignment deepClone() {
		MultipleAlignment ma = new MultipleAlignment(nTaxon, seqLen);
		
		System.arraycopy(names, 0, ma.names, 0, names.length);
		System.arraycopy(data, 0, ma.data, 0, data.length);
		
		ma.buildNameIndex();
		
		return ma;
	}
	
	public MultipleAlignment deepClone( int ntxinc ) {
		MultipleAlignment ma = new MultipleAlignment(nTaxon, seqLen, nTaxon + ntxinc);
		
		System.arraycopy(names, 0, ma.names, 0, names.length);
		System.arraycopy(data, 0, ma.data, 0, data.length);
		
		ma.buildNameIndex();
		
		return ma;
	}

	public static MultipleAlignment loadFasta(File file) {
		// TODO Auto-generated method stub
		try {
			return loadFasta( new FileReader(file));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException( "bailing out.");
		}
	}

	private static MultipleAlignment loadFasta(FileReader fileReader) {
		try {
            BufferedReader r = new BufferedReader(fileReader);

            ArrayList<String>names = new ArrayList<String>();
            ArrayList<String>seqs = new ArrayList<String>();
            
            String curName = null;
            String seqAcc = "";
            String line;
            while( (line = r.readLine()) != null ) {
            	if( line.startsWith(">")) {
            		if( curName != null ) {
            			names.add(curName);
            			seqs.add(seqAcc);
            		}
            		
            		StringTokenizer st = new StringTokenizer(line.substring(1));
            		curName = st.nextToken();
            		seqAcc = "";
            		
            		
            	} else if( curName != null ) {
            		seqAcc += line;
            	}
            }
    		if( curName != null ) {
    			names.add(curName);
    			seqs.add(seqAcc);
    		}
            int len = -1;
    		for( String seq : seqs ) {
    			if( len != -1 && len != seq.length()) {
    				throw new RuntimeException( "sequences in fasta file have different lengths. cannot construct MultipleAlignment" );
    			}
    			
    			len = seq.length();
    		}
            MultipleAlignment ma = new MultipleAlignment(len, names.toArray( new String[names.size()]), seqs.toArray(new String[seqs.size()]));

            r.close();
            return ma;

        } catch (IOException ex) {
            Logger.getLogger(MultipleAlignment.class.getName()).log(Level.SEVERE, null, ex);
            throw new RuntimeException("bailing out");
        }


	}

	public void writeSpecial( PrintStream w, String[] snames, int s, int e ) {
		// TODO Auto-generated method stub
		w.printf( "%d %d\n", e - s, seqLen);

        int maxNameLen = 0;

        for( int i = s; i < e; i++ ) {
            maxNameLen = Math.max( maxNameLen, snames[i].length());
        }

        for( int i = s; i < e; i++ ) {
        	
        	int oidx = nameToIdx(snames[i]);
            w.printf( "%s%s\n", padSpaceRight(names[oidx], maxNameLen + 2), data[oidx]);
        }		
	}
	public void writeSpecial( File f, String[] snames, int s, int e ) {
		try {
			writeSpecial(new PrintStream( new FileOutputStream(f)), snames, s, e);
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			throw new RuntimeException( "bailing out" );
		}
	}
}

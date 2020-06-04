package org.jpac.vioss.s7;

public class DatablockAddress {
	final static String DB    = "DB";
	final static char   BYTE  = 'B'; 
	final static char   WORD  = 'W'; 
	final static char   DWORD = 'D';
	
	public  enum Type       {UNDEFINED, BIT, BYTE, WORD, DWORD};
	
	protected String     addressSpecifier;
	protected Type       type;        
	protected int        bitAddress;  
	protected int        byteAddress;
	protected int        datablockAddress;
	protected int        itemAddress;
	protected int        size;     //size of the data item [byte]
	
	public DatablockAddress() {
		this.type        = type.UNDEFINED;
	}

	public DatablockAddress(String addressSpecifier) throws InvalidAddressSpecifierException {
		this();
		this.addressSpecifier = addressSpecifier.trim().toUpperCase();
		computeAddress(this.addressSpecifier);
	}
	//DB<dbnum>.DB{B|W|D}<inum>[.<bnum>]
	protected void computeAddress(String as) throws InvalidAddressSpecifierException {
		int    i   = 0;
		String num = "";
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		if (!as.startsWith(DB)) {
			throw new InvalidAddressSpecifierException("address must start with datablock specification 'DB<num>'");
		}
		i += DB.length();
		for (; i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9';i++) num += as.charAt(i);//skip blanks
		if (as.charAt(i) != '.' || num.length() == 0 || num.length() > 3 || Integer.valueOf(num) > 32000) {
			throw new InvalidAddressSpecifierException("datablock specification invalid: DB" + num + ". Should be DB<dbnum>");
		}
		datablockAddress = Integer.valueOf(num);
		i++;
		if (!as.substring(i, i+1).equals(DB)) {
			throw new InvalidAddressSpecifierException("datatype specification invalid: " + as.substring(i, i+1) + "... Should be DB{B|W|D}<inum>[.<bnum>]");			
		}
		i += DB.length();
		Type tempType;
		switch(as.charAt(i)) {
			case BYTE:
				tempType = Type.BYTE;
				break;
			case WORD:
				tempType = Type.WORD;
				break;
			case DWORD:
				tempType = Type.DWORD;
				break;
			default:
				throw new InvalidAddressSpecifierException("invalid type specifier: Addressed item must be of type byte ('B'), word ('W') or dword ('D'): " + as.charAt(i));			
		}
		i++;
		num = "";
		for (; i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9';i++) num += as.charAt(i);
		if (num.length() == 0) {
			throw new InvalidAddressSpecifierException("missing address of data item: " + as);						
		}
		itemAddress = Integer.valueOf(num);
		if (as.charAt(i) == '.') {
			//bit address expected
			num = "";
			for (; i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9';i++) num += as.charAt(i);
			if (num.length() == 0                                   || 
				tempType == Type.BYTE  && Integer.valueOf(num) > 7  ||
				tempType == Type.WORD  && Integer.valueOf(num) > 15 ||
				tempType == Type.DWORD && Integer.valueOf(num) > 31   ) {
				throw new InvalidAddressSpecifierException("bit address specification invalid: " + num + ". Should be 0..7 for DBB, 0..15 for DBW or 0..31 for DBD");
			}			
			bitAddress = Integer.valueOf(num);
			type       = Type.BIT;
		} else {
			type = tempType;
		}
		if (i < as.length()) {
			throw new InvalidAddressSpecifierException("trailing part of address specification invalid: " + as.substring(i, as.length()));			
		}
	}
		
	public int getByteAddress() {
		return byteAddress;
	}
	
	public int getBitAddress() {
		return bitAddress;
	}
	
	public int getSize() {
		return this.size;
	}

	public String getAddressSpecifier() {
		return this.addressSpecifier;
	}
	
	public String toString() {
		return getClass().getSimpleName() + "(" + type + ", " + byteAddress + ", " + bitAddress + ")";
	}

    public static void main(String[] args){
        try{          
            DatablockAddress adr =  new DatablockAddress("IW0");              
            System.out.println("IecAddress : " + adr);
            adr =  new DatablockAddress("MW1000");              
            System.out.println("IecAddress : " + adr);
            adr =  new DatablockAddress("MX1000.8");              
            System.out.println("IecAddress : " + adr);
        }
        catch(Error | Exception exc){
            exc.printStackTrace();
        }
     }	
}

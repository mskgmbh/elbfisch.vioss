package org.jpac.vioss.s7;

public class Iec61131Address {
	
	public  enum AccessMode {UNDEFINED, INPUT, OUTPUT, MEMORY};
	public  enum Type       {UNDEFINED, BIT, BYTE, WORD, DWORD};
	
	protected String     addressSpecifier;
	protected AccessMode accessMode;  
	protected Type       type;        
	protected int        bitAddress;  
	protected int        address;
	protected int        dataByteIndex;//byte index in org.jpac.plc.Data
	protected int        dataBitIndex; //bit index in org.jpac.plc.Data
	protected int        dataSize;     //size of the data item [byte]
	
	public Iec61131Address() {
		this.accessMode  = accessMode.UNDEFINED;
		this.type        = type.UNDEFINED;
	}

	public Iec61131Address(String addressSpecifier) throws InvalidAddressSpecifierException {
		this();
		this.addressSpecifier = addressSpecifier.trim().toUpperCase();
		computeAddress(this.addressSpecifier);
	}
	
	protected void computeAddress(String as) throws InvalidAddressSpecifierException {
		int i = 0;
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		char ioDirChar = as.charAt(i);
		switch(ioDirChar) {
		case 'M':
			accessMode = AccessMode.MEMORY;
			break;
		default:
			throw new InvalidAddressSpecifierException("IEC 61131 address specifier: Addressed item must be 'memory' ('M'): " + as);			
		}
		i++;
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		char typeChar = as.charAt(i);
		switch(typeChar) {
		case 'X':
			type = Type.BIT;
			break;		
		case 'B':
			type = Type.BYTE;
			break;
		case 'W':
			type = Type.WORD;
			break;
		case 'D':
			type = Type.DWORD;
			break;
		default:
			throw new InvalidAddressSpecifierException("IEC 61131 address specifier: Addressed item must be of type bit ('X'), byte ('B'), word ('W') or dword ('D'): " + as);			
		}
		i++;
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		address = 0;
		while (i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9') {
			address = 10 * address + as.charAt(i) - '0';
			i++;
		}
		for (; i < as.length() && as.charAt(i) == ' ';i++);//skip blanks
		if (type == Type.BIT) {
			if (as.charAt(i) != '.') {
				throw new InvalidAddressSpecifierException("IEC 61131 address must contain a bit address: " + as);
			}
			i++;
			bitAddress = 0;
			if (!(i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9')) {
				throw new InvalidAddressSpecifierException("IEC 61131 address must contain a bit address: " + as);
			}
			bitAddress = as.charAt(i) - '0';
			i++;
			if ((i < as.length() && as.charAt(i) >= '0' && as.charAt(i) <= '9')) {
				bitAddress = 10 * bitAddress + as.charAt(i) - '0';
			}
			if (bitAddress > 7) {
				throw new InvalidAddressSpecifierException("IEC 61131 bit address must be in range of 0..7: " + as);
			}
		} else {
			if (i < as.length() && as.charAt(i) == '.') {
				throw new InvalidAddressSpecifierException("IEC 61131 address must not contain a bit address: " + as);
			}			
		}

		//provide data byte and bit index for accessing org.jpac.plc.Data
		//considering the byte order of the modbus protocol being big endian 
		switch(type) {
			case BIT:
				dataByteIndex = address;//address is byte address
				dataBitIndex  = bitAddress;
				dataSize      = 1;
				break;
			case BYTE:
				dataSize      = 1;
				dataByteIndex = address;//address is byte address
				dataBitIndex  = 0;
				break;
			case WORD:
				dataSize      = 2;
				dataByteIndex = address;//address is byte address
				dataBitIndex  = 0;
			case DWORD:
				dataSize      = 4;
				dataByteIndex = address;//address is byte address
				dataBitIndex  = 0;
				break;
			default:
				//cannot happen
				break;
		}
	}
	
	public AccessMode getAccessMode() {
		return accessMode;
	}
	
	public int getAddress() {
		return address;
	}
	
	public int getBitAddress() {
		return bitAddress;
	}
	
	public int getDataByteIndex() {
		return this.dataByteIndex;
	}

	public int getDataBitIndex() {
		return this.dataBitIndex;
	}
	
	public int getDataSize() {
		return this.dataSize;
	}

	public String getAddressSpecifier() {
		return this.addressSpecifier;
	}
	
	public String toString() {
		return getClass().getSimpleName() + "(" + accessMode + ", " + type + ", " + address + ", " + bitAddress + ", " + dataByteIndex + ", " + dataBitIndex + ")";
	}

    public static void main(String[] args){
        try{          
            Iec61131Address adr =  new Iec61131Address("IW0");              
            System.out.println("IecAddress : " + adr);
            adr =  new Iec61131Address("MW1000");              
            System.out.println("IecAddress : " + adr);
            adr =  new Iec61131Address("MX1000.8");              
            System.out.println("IecAddress : " + adr);
        }
        catch(Error | Exception exc){
            exc.printStackTrace();
        }
     }	
}

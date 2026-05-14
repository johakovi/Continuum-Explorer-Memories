package com.troikoss.continuum_explorer.model

import android.os.Parcel
import android.os.Parcelable

class ShizukuFileInfo() : Parcelable {
    var name: String = ""
    var isDirectory: Boolean = false
    var size: Long = 0L
    var lastModified: Long = 0L

    constructor(name: String, isDirectory: Boolean, size: Long, lastModified: Long) : this() {
        this.name = name
        this.isDirectory = isDirectory
        this.size = size
        this.lastModified = lastModified
    }

    constructor(parcel: Parcel) : this() {
        name = parcel.readString() ?: ""
        isDirectory = parcel.readByte() != 0.toByte()
        size = parcel.readLong()
        lastModified = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeByte(if (isDirectory) 1 else 0)
        parcel.writeLong(size)
        parcel.writeLong(lastModified)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ShizukuFileInfo> {
        override fun createFromParcel(parcel: Parcel): ShizukuFileInfo = ShizukuFileInfo(parcel)
        override fun newArray(size: Int): Array<ShizukuFileInfo?> = arrayOfNulls(size)
    }
}

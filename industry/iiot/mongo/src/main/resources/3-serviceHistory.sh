# Script is sourced, can't use $0
MYNAME=3-serviceHistory.sh
echo ${MYNAME} : 'START - - - - - - - - - - - - - - - - - - - - - - - - - - - -'
mongosh -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
print("===================")
db.@my.mongo.collection1.name@.insert( 
   [
     { _id: "01", lastServicedOffset: -15 },
     { _id: "02", lastServicedOffset: -12 },
     { _id: "03", lastServicedOffset: -81 },
     { _id: "04", lastServicedOffset: -33 },
     { _id: "05", lastServicedOffset: -122 },
     { _id: "06", lastServicedOffset: -201 },
     { _id: "07", lastServicedOffset: -14 },
     { _id: "08", lastServicedOffset: -55 },
     { _id: "09", lastServicedOffset: -23 },
     { _id: "10", lastServicedOffset: -42 },
     { _id: "11", lastServicedOffset: -83 },
     { _id: "12", lastServicedOffset: -142 }
   ] )   
EOF
mongosh -u @my.other.admin.user@ -p @my.other.admin.password@ << EOF
use @my.other.admin.database@
print("===================")
print("db.@my.mongo.collection1.name@.countDocuments() - Size of the '@my.mongo.collection1.name@' collection in the '@my.other.admin.database@' database")
db.@my.mongo.collection1.name@.countDocuments()
EOF
echo ${MYNAME} : ' END - - - - - - - - - - - - - - - - - - - - - - - - - - - -'

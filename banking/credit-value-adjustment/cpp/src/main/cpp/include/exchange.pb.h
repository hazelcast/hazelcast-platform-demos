// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: exchange.proto

#ifndef GOOGLE_PROTOBUF_INCLUDED_exchange_2eproto
#define GOOGLE_PROTOBUF_INCLUDED_exchange_2eproto

#include <limits>
#include <string>

#include <google/protobuf/port_def.inc>
#if PROTOBUF_VERSION < 3012000
#error This file was generated by a newer version of protoc which is
#error incompatible with your Protocol Buffer headers. Please update
#error your headers.
#endif
#if 3012002 < PROTOBUF_MIN_PROTOC_VERSION
#error This file was generated by an older version of protoc which is
#error incompatible with your Protocol Buffer headers. Please
#error regenerate this file with a newer version of protoc.
#endif

#include <google/protobuf/port_undef.inc>
#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/arena.h>
#include <google/protobuf/arenastring.h>
#include <google/protobuf/generated_message_table_driven.h>
#include <google/protobuf/generated_message_util.h>
#include <google/protobuf/inlined_string_field.h>
#include <google/protobuf/metadata_lite.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/message.h>
#include <google/protobuf/repeated_field.h>  // IWYU pragma: export
#include <google/protobuf/extension_set.h>  // IWYU pragma: export
#include <google/protobuf/unknown_field_set.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>
#define PROTOBUF_INTERNAL_EXPORT_exchange_2eproto
PROTOBUF_NAMESPACE_OPEN
namespace internal {
class AnyMetadata;
}  // namespace internal
PROTOBUF_NAMESPACE_CLOSE

// Internal implementation detail -- do not use these members.
struct TableStruct_exchange_2eproto {
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTableField entries[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::AuxillaryParseTableField aux[]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::ParseTable schema[1]
    PROTOBUF_SECTION_VARIABLE(protodesc_cold);
  static const ::PROTOBUF_NAMESPACE_ID::internal::FieldMetadata field_metadata[];
  static const ::PROTOBUF_NAMESPACE_ID::internal::SerializationTable serialization_table[];
  static const ::PROTOBUF_NAMESPACE_ID::uint32 offsets[];
};
extern const ::PROTOBUF_NAMESPACE_ID::internal::DescriptorTable descriptor_table_exchange_2eproto;
namespace FlumaionQL {
class DataExchange;
class DataExchangeDefaultTypeInternal;
extern DataExchangeDefaultTypeInternal _DataExchange_default_instance_;
}  // namespace FlumaionQL
PROTOBUF_NAMESPACE_OPEN
template<> ::FlumaionQL::DataExchange* Arena::CreateMaybeMessage<::FlumaionQL::DataExchange>(Arena*);
PROTOBUF_NAMESPACE_CLOSE
namespace FlumaionQL {

// ===================================================================

class DataExchange PROTOBUF_FINAL :
    public ::PROTOBUF_NAMESPACE_ID::Message /* @@protoc_insertion_point(class_definition:FlumaionQL.DataExchange) */ {
 public:
  inline DataExchange() : DataExchange(nullptr) {};
  virtual ~DataExchange();

  DataExchange(const DataExchange& from);
  DataExchange(DataExchange&& from) noexcept
    : DataExchange() {
    *this = ::std::move(from);
  }

  inline DataExchange& operator=(const DataExchange& from) {
    CopyFrom(from);
    return *this;
  }
  inline DataExchange& operator=(DataExchange&& from) noexcept {
    if (GetArena() == from.GetArena()) {
      if (this != &from) InternalSwap(&from);
    } else {
      CopyFrom(from);
    }
    return *this;
  }

  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* descriptor() {
    return GetDescriptor();
  }
  static const ::PROTOBUF_NAMESPACE_ID::Descriptor* GetDescriptor() {
    return GetMetadataStatic().descriptor;
  }
  static const ::PROTOBUF_NAMESPACE_ID::Reflection* GetReflection() {
    return GetMetadataStatic().reflection;
  }
  static const DataExchange& default_instance();

  static void InitAsDefaultInstance();  // FOR INTERNAL USE ONLY
  static inline const DataExchange* internal_default_instance() {
    return reinterpret_cast<const DataExchange*>(
               &_DataExchange_default_instance_);
  }
  static constexpr int kIndexInFileMessages =
    0;

  friend void swap(DataExchange& a, DataExchange& b) {
    a.Swap(&b);
  }
  inline void Swap(DataExchange* other) {
    if (other == this) return;
    if (GetArena() == other->GetArena()) {
      InternalSwap(other);
    } else {
      ::PROTOBUF_NAMESPACE_ID::internal::GenericSwap(this, other);
    }
  }
  void UnsafeArenaSwap(DataExchange* other) {
    if (other == this) return;
    GOOGLE_DCHECK(GetArena() == other->GetArena());
    InternalSwap(other);
  }

  // implements Message ----------------------------------------------

  inline DataExchange* New() const final {
    return CreateMaybeMessage<DataExchange>(nullptr);
  }

  DataExchange* New(::PROTOBUF_NAMESPACE_ID::Arena* arena) const final {
    return CreateMaybeMessage<DataExchange>(arena);
  }
  void CopyFrom(const ::PROTOBUF_NAMESPACE_ID::Message& from) final;
  void MergeFrom(const ::PROTOBUF_NAMESPACE_ID::Message& from) final;
  void CopyFrom(const DataExchange& from);
  void MergeFrom(const DataExchange& from);
  PROTOBUF_ATTRIBUTE_REINITIALIZES void Clear() final;
  bool IsInitialized() const final;

  size_t ByteSizeLong() const final;
  const char* _InternalParse(const char* ptr, ::PROTOBUF_NAMESPACE_ID::internal::ParseContext* ctx) final;
  ::PROTOBUF_NAMESPACE_ID::uint8* _InternalSerialize(
      ::PROTOBUF_NAMESPACE_ID::uint8* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const final;
  int GetCachedSize() const final { return _cached_size_.Get(); }

  private:
  inline void SharedCtor();
  inline void SharedDtor();
  void SetCachedSize(int size) const final;
  void InternalSwap(DataExchange* other);
  friend class ::PROTOBUF_NAMESPACE_ID::internal::AnyMetadata;
  static ::PROTOBUF_NAMESPACE_ID::StringPiece FullMessageName() {
    return "FlumaionQL.DataExchange";
  }
  protected:
  explicit DataExchange(::PROTOBUF_NAMESPACE_ID::Arena* arena);
  private:
  static void ArenaDtor(void* object);
  inline void RegisterArenaDtor(::PROTOBUF_NAMESPACE_ID::Arena* arena);
  public:

  ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadata() const final;
  private:
  static ::PROTOBUF_NAMESPACE_ID::Metadata GetMetadataStatic() {
    ::PROTOBUF_NAMESPACE_ID::internal::AssignDescriptors(&::descriptor_table_exchange_2eproto);
    return ::descriptor_table_exchange_2eproto.file_level_metadata[kIndexInFileMessages];
  }

  public:

  // nested types ----------------------------------------------------

  // accessors -------------------------------------------------------

  enum : int {
    kCurvefileFieldNumber = 1,
    kFixingfileFieldNumber = 2,
    kTradefileFieldNumber = 3,
    kEvaldateFieldNumber = 4,
    kResultsfileFieldNumber = 5,
    kScenarioidFieldNumber = 6,
    kTradeidFieldNumber = 7,
  };
  // string curvefile = 1;
  void clear_curvefile();
  const std::string& curvefile() const;
  void set_curvefile(const std::string& value);
  void set_curvefile(std::string&& value);
  void set_curvefile(const char* value);
  void set_curvefile(const char* value, size_t size);
  std::string* mutable_curvefile();
  std::string* release_curvefile();
  void set_allocated_curvefile(std::string* curvefile);
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  std::string* unsafe_arena_release_curvefile();
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  void unsafe_arena_set_allocated_curvefile(
      std::string* curvefile);
  private:
  const std::string& _internal_curvefile() const;
  void _internal_set_curvefile(const std::string& value);
  std::string* _internal_mutable_curvefile();
  public:

  // string fixingfile = 2;
  void clear_fixingfile();
  const std::string& fixingfile() const;
  void set_fixingfile(const std::string& value);
  void set_fixingfile(std::string&& value);
  void set_fixingfile(const char* value);
  void set_fixingfile(const char* value, size_t size);
  std::string* mutable_fixingfile();
  std::string* release_fixingfile();
  void set_allocated_fixingfile(std::string* fixingfile);
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  std::string* unsafe_arena_release_fixingfile();
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  void unsafe_arena_set_allocated_fixingfile(
      std::string* fixingfile);
  private:
  const std::string& _internal_fixingfile() const;
  void _internal_set_fixingfile(const std::string& value);
  std::string* _internal_mutable_fixingfile();
  public:

  // string tradefile = 3;
  void clear_tradefile();
  const std::string& tradefile() const;
  void set_tradefile(const std::string& value);
  void set_tradefile(std::string&& value);
  void set_tradefile(const char* value);
  void set_tradefile(const char* value, size_t size);
  std::string* mutable_tradefile();
  std::string* release_tradefile();
  void set_allocated_tradefile(std::string* tradefile);
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  std::string* unsafe_arena_release_tradefile();
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  void unsafe_arena_set_allocated_tradefile(
      std::string* tradefile);
  private:
  const std::string& _internal_tradefile() const;
  void _internal_set_tradefile(const std::string& value);
  std::string* _internal_mutable_tradefile();
  public:

  // string evaldate = 4;
  void clear_evaldate();
  const std::string& evaldate() const;
  void set_evaldate(const std::string& value);
  void set_evaldate(std::string&& value);
  void set_evaldate(const char* value);
  void set_evaldate(const char* value, size_t size);
  std::string* mutable_evaldate();
  std::string* release_evaldate();
  void set_allocated_evaldate(std::string* evaldate);
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  std::string* unsafe_arena_release_evaldate();
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  void unsafe_arena_set_allocated_evaldate(
      std::string* evaldate);
  private:
  const std::string& _internal_evaldate() const;
  void _internal_set_evaldate(const std::string& value);
  std::string* _internal_mutable_evaldate();
  public:

  // string resultsfile = 5;
  void clear_resultsfile();
  const std::string& resultsfile() const;
  void set_resultsfile(const std::string& value);
  void set_resultsfile(std::string&& value);
  void set_resultsfile(const char* value);
  void set_resultsfile(const char* value, size_t size);
  std::string* mutable_resultsfile();
  std::string* release_resultsfile();
  void set_allocated_resultsfile(std::string* resultsfile);
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  std::string* unsafe_arena_release_resultsfile();
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  void unsafe_arena_set_allocated_resultsfile(
      std::string* resultsfile);
  private:
  const std::string& _internal_resultsfile() const;
  void _internal_set_resultsfile(const std::string& value);
  std::string* _internal_mutable_resultsfile();
  public:

  // string scenarioid = 6;
  void clear_scenarioid();
  const std::string& scenarioid() const;
  void set_scenarioid(const std::string& value);
  void set_scenarioid(std::string&& value);
  void set_scenarioid(const char* value);
  void set_scenarioid(const char* value, size_t size);
  std::string* mutable_scenarioid();
  std::string* release_scenarioid();
  void set_allocated_scenarioid(std::string* scenarioid);
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  std::string* unsafe_arena_release_scenarioid();
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  void unsafe_arena_set_allocated_scenarioid(
      std::string* scenarioid);
  private:
  const std::string& _internal_scenarioid() const;
  void _internal_set_scenarioid(const std::string& value);
  std::string* _internal_mutable_scenarioid();
  public:

  // string tradeid = 7;
  void clear_tradeid();
  const std::string& tradeid() const;
  void set_tradeid(const std::string& value);
  void set_tradeid(std::string&& value);
  void set_tradeid(const char* value);
  void set_tradeid(const char* value, size_t size);
  std::string* mutable_tradeid();
  std::string* release_tradeid();
  void set_allocated_tradeid(std::string* tradeid);
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  std::string* unsafe_arena_release_tradeid();
  GOOGLE_PROTOBUF_RUNTIME_DEPRECATED("The unsafe_arena_ accessors for"
  "    string fields are deprecated and will be removed in a"
  "    future release.")
  void unsafe_arena_set_allocated_tradeid(
      std::string* tradeid);
  private:
  const std::string& _internal_tradeid() const;
  void _internal_set_tradeid(const std::string& value);
  std::string* _internal_mutable_tradeid();
  public:

  // @@protoc_insertion_point(class_scope:FlumaionQL.DataExchange)
 private:
  class _Internal;

  template <typename T> friend class ::PROTOBUF_NAMESPACE_ID::Arena::InternalHelper;
  typedef void InternalArenaConstructable_;
  typedef void DestructorSkippable_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr curvefile_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr fixingfile_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr tradefile_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr evaldate_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr resultsfile_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr scenarioid_;
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr tradeid_;
  mutable ::PROTOBUF_NAMESPACE_ID::internal::CachedSize _cached_size_;
  friend struct ::TableStruct_exchange_2eproto;
};
// ===================================================================


// ===================================================================

#ifdef __GNUC__
  #pragma GCC diagnostic push
  #pragma GCC diagnostic ignored "-Wstrict-aliasing"
#endif  // __GNUC__
// DataExchange

// string curvefile = 1;
inline void DataExchange::clear_curvefile() {
  curvefile_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline const std::string& DataExchange::curvefile() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.DataExchange.curvefile)
  return _internal_curvefile();
}
inline void DataExchange::set_curvefile(const std::string& value) {
  _internal_set_curvefile(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.DataExchange.curvefile)
}
inline std::string* DataExchange::mutable_curvefile() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.DataExchange.curvefile)
  return _internal_mutable_curvefile();
}
inline const std::string& DataExchange::_internal_curvefile() const {
  return curvefile_.Get();
}
inline void DataExchange::_internal_set_curvefile(const std::string& value) {
  
  curvefile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value, GetArena());
}
inline void DataExchange::set_curvefile(std::string&& value) {
  
  curvefile_.Set(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value), GetArena());
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.DataExchange.curvefile)
}
inline void DataExchange::set_curvefile(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  curvefile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value),
              GetArena());
  // @@protoc_insertion_point(field_set_char:FlumaionQL.DataExchange.curvefile)
}
inline void DataExchange::set_curvefile(const char* value,
    size_t size) {
  
  curvefile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(
      reinterpret_cast<const char*>(value), size), GetArena());
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.DataExchange.curvefile)
}
inline std::string* DataExchange::_internal_mutable_curvefile() {
  
  return curvefile_.Mutable(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline std::string* DataExchange::release_curvefile() {
  // @@protoc_insertion_point(field_release:FlumaionQL.DataExchange.curvefile)
  return curvefile_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline void DataExchange::set_allocated_curvefile(std::string* curvefile) {
  if (curvefile != nullptr) {
    
  } else {
    
  }
  curvefile_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), curvefile,
      GetArena());
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.DataExchange.curvefile)
}
inline std::string* DataExchange::unsafe_arena_release_curvefile() {
  // @@protoc_insertion_point(field_unsafe_arena_release:FlumaionQL.DataExchange.curvefile)
  GOOGLE_DCHECK(GetArena() != nullptr);
  
  return curvefile_.UnsafeArenaRelease(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      GetArena());
}
inline void DataExchange::unsafe_arena_set_allocated_curvefile(
    std::string* curvefile) {
  GOOGLE_DCHECK(GetArena() != nullptr);
  if (curvefile != nullptr) {
    
  } else {
    
  }
  curvefile_.UnsafeArenaSetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      curvefile, GetArena());
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:FlumaionQL.DataExchange.curvefile)
}

// string fixingfile = 2;
inline void DataExchange::clear_fixingfile() {
  fixingfile_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline const std::string& DataExchange::fixingfile() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.DataExchange.fixingfile)
  return _internal_fixingfile();
}
inline void DataExchange::set_fixingfile(const std::string& value) {
  _internal_set_fixingfile(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.DataExchange.fixingfile)
}
inline std::string* DataExchange::mutable_fixingfile() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.DataExchange.fixingfile)
  return _internal_mutable_fixingfile();
}
inline const std::string& DataExchange::_internal_fixingfile() const {
  return fixingfile_.Get();
}
inline void DataExchange::_internal_set_fixingfile(const std::string& value) {
  
  fixingfile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value, GetArena());
}
inline void DataExchange::set_fixingfile(std::string&& value) {
  
  fixingfile_.Set(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value), GetArena());
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.DataExchange.fixingfile)
}
inline void DataExchange::set_fixingfile(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  fixingfile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value),
              GetArena());
  // @@protoc_insertion_point(field_set_char:FlumaionQL.DataExchange.fixingfile)
}
inline void DataExchange::set_fixingfile(const char* value,
    size_t size) {
  
  fixingfile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(
      reinterpret_cast<const char*>(value), size), GetArena());
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.DataExchange.fixingfile)
}
inline std::string* DataExchange::_internal_mutable_fixingfile() {
  
  return fixingfile_.Mutable(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline std::string* DataExchange::release_fixingfile() {
  // @@protoc_insertion_point(field_release:FlumaionQL.DataExchange.fixingfile)
  return fixingfile_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline void DataExchange::set_allocated_fixingfile(std::string* fixingfile) {
  if (fixingfile != nullptr) {
    
  } else {
    
  }
  fixingfile_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), fixingfile,
      GetArena());
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.DataExchange.fixingfile)
}
inline std::string* DataExchange::unsafe_arena_release_fixingfile() {
  // @@protoc_insertion_point(field_unsafe_arena_release:FlumaionQL.DataExchange.fixingfile)
  GOOGLE_DCHECK(GetArena() != nullptr);
  
  return fixingfile_.UnsafeArenaRelease(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      GetArena());
}
inline void DataExchange::unsafe_arena_set_allocated_fixingfile(
    std::string* fixingfile) {
  GOOGLE_DCHECK(GetArena() != nullptr);
  if (fixingfile != nullptr) {
    
  } else {
    
  }
  fixingfile_.UnsafeArenaSetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      fixingfile, GetArena());
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:FlumaionQL.DataExchange.fixingfile)
}

// string tradefile = 3;
inline void DataExchange::clear_tradefile() {
  tradefile_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline const std::string& DataExchange::tradefile() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.DataExchange.tradefile)
  return _internal_tradefile();
}
inline void DataExchange::set_tradefile(const std::string& value) {
  _internal_set_tradefile(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.DataExchange.tradefile)
}
inline std::string* DataExchange::mutable_tradefile() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.DataExchange.tradefile)
  return _internal_mutable_tradefile();
}
inline const std::string& DataExchange::_internal_tradefile() const {
  return tradefile_.Get();
}
inline void DataExchange::_internal_set_tradefile(const std::string& value) {
  
  tradefile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value, GetArena());
}
inline void DataExchange::set_tradefile(std::string&& value) {
  
  tradefile_.Set(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value), GetArena());
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.DataExchange.tradefile)
}
inline void DataExchange::set_tradefile(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  tradefile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value),
              GetArena());
  // @@protoc_insertion_point(field_set_char:FlumaionQL.DataExchange.tradefile)
}
inline void DataExchange::set_tradefile(const char* value,
    size_t size) {
  
  tradefile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(
      reinterpret_cast<const char*>(value), size), GetArena());
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.DataExchange.tradefile)
}
inline std::string* DataExchange::_internal_mutable_tradefile() {
  
  return tradefile_.Mutable(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline std::string* DataExchange::release_tradefile() {
  // @@protoc_insertion_point(field_release:FlumaionQL.DataExchange.tradefile)
  return tradefile_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline void DataExchange::set_allocated_tradefile(std::string* tradefile) {
  if (tradefile != nullptr) {
    
  } else {
    
  }
  tradefile_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), tradefile,
      GetArena());
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.DataExchange.tradefile)
}
inline std::string* DataExchange::unsafe_arena_release_tradefile() {
  // @@protoc_insertion_point(field_unsafe_arena_release:FlumaionQL.DataExchange.tradefile)
  GOOGLE_DCHECK(GetArena() != nullptr);
  
  return tradefile_.UnsafeArenaRelease(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      GetArena());
}
inline void DataExchange::unsafe_arena_set_allocated_tradefile(
    std::string* tradefile) {
  GOOGLE_DCHECK(GetArena() != nullptr);
  if (tradefile != nullptr) {
    
  } else {
    
  }
  tradefile_.UnsafeArenaSetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      tradefile, GetArena());
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:FlumaionQL.DataExchange.tradefile)
}

// string evaldate = 4;
inline void DataExchange::clear_evaldate() {
  evaldate_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline const std::string& DataExchange::evaldate() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.DataExchange.evaldate)
  return _internal_evaldate();
}
inline void DataExchange::set_evaldate(const std::string& value) {
  _internal_set_evaldate(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.DataExchange.evaldate)
}
inline std::string* DataExchange::mutable_evaldate() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.DataExchange.evaldate)
  return _internal_mutable_evaldate();
}
inline const std::string& DataExchange::_internal_evaldate() const {
  return evaldate_.Get();
}
inline void DataExchange::_internal_set_evaldate(const std::string& value) {
  
  evaldate_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value, GetArena());
}
inline void DataExchange::set_evaldate(std::string&& value) {
  
  evaldate_.Set(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value), GetArena());
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.DataExchange.evaldate)
}
inline void DataExchange::set_evaldate(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  evaldate_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value),
              GetArena());
  // @@protoc_insertion_point(field_set_char:FlumaionQL.DataExchange.evaldate)
}
inline void DataExchange::set_evaldate(const char* value,
    size_t size) {
  
  evaldate_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(
      reinterpret_cast<const char*>(value), size), GetArena());
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.DataExchange.evaldate)
}
inline std::string* DataExchange::_internal_mutable_evaldate() {
  
  return evaldate_.Mutable(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline std::string* DataExchange::release_evaldate() {
  // @@protoc_insertion_point(field_release:FlumaionQL.DataExchange.evaldate)
  return evaldate_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline void DataExchange::set_allocated_evaldate(std::string* evaldate) {
  if (evaldate != nullptr) {
    
  } else {
    
  }
  evaldate_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), evaldate,
      GetArena());
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.DataExchange.evaldate)
}
inline std::string* DataExchange::unsafe_arena_release_evaldate() {
  // @@protoc_insertion_point(field_unsafe_arena_release:FlumaionQL.DataExchange.evaldate)
  GOOGLE_DCHECK(GetArena() != nullptr);
  
  return evaldate_.UnsafeArenaRelease(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      GetArena());
}
inline void DataExchange::unsafe_arena_set_allocated_evaldate(
    std::string* evaldate) {
  GOOGLE_DCHECK(GetArena() != nullptr);
  if (evaldate != nullptr) {
    
  } else {
    
  }
  evaldate_.UnsafeArenaSetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      evaldate, GetArena());
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:FlumaionQL.DataExchange.evaldate)
}

// string resultsfile = 5;
inline void DataExchange::clear_resultsfile() {
  resultsfile_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline const std::string& DataExchange::resultsfile() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.DataExchange.resultsfile)
  return _internal_resultsfile();
}
inline void DataExchange::set_resultsfile(const std::string& value) {
  _internal_set_resultsfile(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.DataExchange.resultsfile)
}
inline std::string* DataExchange::mutable_resultsfile() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.DataExchange.resultsfile)
  return _internal_mutable_resultsfile();
}
inline const std::string& DataExchange::_internal_resultsfile() const {
  return resultsfile_.Get();
}
inline void DataExchange::_internal_set_resultsfile(const std::string& value) {
  
  resultsfile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value, GetArena());
}
inline void DataExchange::set_resultsfile(std::string&& value) {
  
  resultsfile_.Set(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value), GetArena());
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.DataExchange.resultsfile)
}
inline void DataExchange::set_resultsfile(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  resultsfile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value),
              GetArena());
  // @@protoc_insertion_point(field_set_char:FlumaionQL.DataExchange.resultsfile)
}
inline void DataExchange::set_resultsfile(const char* value,
    size_t size) {
  
  resultsfile_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(
      reinterpret_cast<const char*>(value), size), GetArena());
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.DataExchange.resultsfile)
}
inline std::string* DataExchange::_internal_mutable_resultsfile() {
  
  return resultsfile_.Mutable(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline std::string* DataExchange::release_resultsfile() {
  // @@protoc_insertion_point(field_release:FlumaionQL.DataExchange.resultsfile)
  return resultsfile_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline void DataExchange::set_allocated_resultsfile(std::string* resultsfile) {
  if (resultsfile != nullptr) {
    
  } else {
    
  }
  resultsfile_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), resultsfile,
      GetArena());
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.DataExchange.resultsfile)
}
inline std::string* DataExchange::unsafe_arena_release_resultsfile() {
  // @@protoc_insertion_point(field_unsafe_arena_release:FlumaionQL.DataExchange.resultsfile)
  GOOGLE_DCHECK(GetArena() != nullptr);
  
  return resultsfile_.UnsafeArenaRelease(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      GetArena());
}
inline void DataExchange::unsafe_arena_set_allocated_resultsfile(
    std::string* resultsfile) {
  GOOGLE_DCHECK(GetArena() != nullptr);
  if (resultsfile != nullptr) {
    
  } else {
    
  }
  resultsfile_.UnsafeArenaSetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      resultsfile, GetArena());
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:FlumaionQL.DataExchange.resultsfile)
}

// string scenarioid = 6;
inline void DataExchange::clear_scenarioid() {
  scenarioid_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline const std::string& DataExchange::scenarioid() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.DataExchange.scenarioid)
  return _internal_scenarioid();
}
inline void DataExchange::set_scenarioid(const std::string& value) {
  _internal_set_scenarioid(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.DataExchange.scenarioid)
}
inline std::string* DataExchange::mutable_scenarioid() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.DataExchange.scenarioid)
  return _internal_mutable_scenarioid();
}
inline const std::string& DataExchange::_internal_scenarioid() const {
  return scenarioid_.Get();
}
inline void DataExchange::_internal_set_scenarioid(const std::string& value) {
  
  scenarioid_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value, GetArena());
}
inline void DataExchange::set_scenarioid(std::string&& value) {
  
  scenarioid_.Set(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value), GetArena());
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.DataExchange.scenarioid)
}
inline void DataExchange::set_scenarioid(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  scenarioid_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value),
              GetArena());
  // @@protoc_insertion_point(field_set_char:FlumaionQL.DataExchange.scenarioid)
}
inline void DataExchange::set_scenarioid(const char* value,
    size_t size) {
  
  scenarioid_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(
      reinterpret_cast<const char*>(value), size), GetArena());
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.DataExchange.scenarioid)
}
inline std::string* DataExchange::_internal_mutable_scenarioid() {
  
  return scenarioid_.Mutable(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline std::string* DataExchange::release_scenarioid() {
  // @@protoc_insertion_point(field_release:FlumaionQL.DataExchange.scenarioid)
  return scenarioid_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline void DataExchange::set_allocated_scenarioid(std::string* scenarioid) {
  if (scenarioid != nullptr) {
    
  } else {
    
  }
  scenarioid_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), scenarioid,
      GetArena());
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.DataExchange.scenarioid)
}
inline std::string* DataExchange::unsafe_arena_release_scenarioid() {
  // @@protoc_insertion_point(field_unsafe_arena_release:FlumaionQL.DataExchange.scenarioid)
  GOOGLE_DCHECK(GetArena() != nullptr);
  
  return scenarioid_.UnsafeArenaRelease(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      GetArena());
}
inline void DataExchange::unsafe_arena_set_allocated_scenarioid(
    std::string* scenarioid) {
  GOOGLE_DCHECK(GetArena() != nullptr);
  if (scenarioid != nullptr) {
    
  } else {
    
  }
  scenarioid_.UnsafeArenaSetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      scenarioid, GetArena());
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:FlumaionQL.DataExchange.scenarioid)
}

// string tradeid = 7;
inline void DataExchange::clear_tradeid() {
  tradeid_.ClearToEmpty(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline const std::string& DataExchange::tradeid() const {
  // @@protoc_insertion_point(field_get:FlumaionQL.DataExchange.tradeid)
  return _internal_tradeid();
}
inline void DataExchange::set_tradeid(const std::string& value) {
  _internal_set_tradeid(value);
  // @@protoc_insertion_point(field_set:FlumaionQL.DataExchange.tradeid)
}
inline std::string* DataExchange::mutable_tradeid() {
  // @@protoc_insertion_point(field_mutable:FlumaionQL.DataExchange.tradeid)
  return _internal_mutable_tradeid();
}
inline const std::string& DataExchange::_internal_tradeid() const {
  return tradeid_.Get();
}
inline void DataExchange::_internal_set_tradeid(const std::string& value) {
  
  tradeid_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), value, GetArena());
}
inline void DataExchange::set_tradeid(std::string&& value) {
  
  tradeid_.Set(
    &::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::move(value), GetArena());
  // @@protoc_insertion_point(field_set_rvalue:FlumaionQL.DataExchange.tradeid)
}
inline void DataExchange::set_tradeid(const char* value) {
  GOOGLE_DCHECK(value != nullptr);
  
  tradeid_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(value),
              GetArena());
  // @@protoc_insertion_point(field_set_char:FlumaionQL.DataExchange.tradeid)
}
inline void DataExchange::set_tradeid(const char* value,
    size_t size) {
  
  tradeid_.Set(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), ::std::string(
      reinterpret_cast<const char*>(value), size), GetArena());
  // @@protoc_insertion_point(field_set_pointer:FlumaionQL.DataExchange.tradeid)
}
inline std::string* DataExchange::_internal_mutable_tradeid() {
  
  return tradeid_.Mutable(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline std::string* DataExchange::release_tradeid() {
  // @@protoc_insertion_point(field_release:FlumaionQL.DataExchange.tradeid)
  return tradeid_.Release(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), GetArena());
}
inline void DataExchange::set_allocated_tradeid(std::string* tradeid) {
  if (tradeid != nullptr) {
    
  } else {
    
  }
  tradeid_.SetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(), tradeid,
      GetArena());
  // @@protoc_insertion_point(field_set_allocated:FlumaionQL.DataExchange.tradeid)
}
inline std::string* DataExchange::unsafe_arena_release_tradeid() {
  // @@protoc_insertion_point(field_unsafe_arena_release:FlumaionQL.DataExchange.tradeid)
  GOOGLE_DCHECK(GetArena() != nullptr);
  
  return tradeid_.UnsafeArenaRelease(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      GetArena());
}
inline void DataExchange::unsafe_arena_set_allocated_tradeid(
    std::string* tradeid) {
  GOOGLE_DCHECK(GetArena() != nullptr);
  if (tradeid != nullptr) {
    
  } else {
    
  }
  tradeid_.UnsafeArenaSetAllocated(&::PROTOBUF_NAMESPACE_ID::internal::GetEmptyStringAlreadyInited(),
      tradeid, GetArena());
  // @@protoc_insertion_point(field_unsafe_arena_set_allocated:FlumaionQL.DataExchange.tradeid)
}

#ifdef __GNUC__
  #pragma GCC diagnostic pop
#endif  // __GNUC__

// @@protoc_insertion_point(namespace_scope)

}  // namespace FlumaionQL

// @@protoc_insertion_point(global_scope)

#include <google/protobuf/port_undef.inc>
#endif  // GOOGLE_PROTOBUF_INCLUDED_GOOGLE_PROTOBUF_INCLUDED_exchange_2eproto
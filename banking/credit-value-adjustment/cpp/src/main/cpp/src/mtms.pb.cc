// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: mtms.proto

#include "../include/mtms.pb.h"

#include <algorithm>

#include <google/protobuf/io/coded_stream.h>
#include <google/protobuf/extension_set.h>
#include <google/protobuf/wire_format_lite.h>
#include <google/protobuf/descriptor.h>
#include <google/protobuf/generated_message_reflection.h>
#include <google/protobuf/reflection_ops.h>
#include <google/protobuf/wire_format.h>
// @@protoc_insertion_point(includes)
#include <google/protobuf/port_def.inc>

PROTOBUF_PRAGMA_INIT_SEG

namespace _pb = ::PROTOBUF_NAMESPACE_ID;
namespace _pbi = _pb::internal;

namespace FlumaionQL {
PROTOBUF_CONSTEXPR MTM::MTM(
    ::_pbi::ConstantInitialized): _impl_{
    /*decltype(_impl_.fixlegdates_)*/{}
  , /*decltype(_impl_._fixlegdates_cached_byte_size_)*/{0}
  , /*decltype(_impl_.fixlegamount_)*/{}
  , /*decltype(_impl_.fltlegdates_)*/{}
  , /*decltype(_impl_._fltlegdates_cached_byte_size_)*/{0}
  , /*decltype(_impl_.fltlegamount_)*/{}
  , /*decltype(_impl_.discountvalues_)*/{}
  , /*decltype(_impl_.legfractions_)*/{}
  , /*decltype(_impl_.tradeid_)*/{&::_pbi::fixed_address_empty_string, ::_pbi::ConstantInitialized{}}
  , /*decltype(_impl_.curvename_)*/{&::_pbi::fixed_address_empty_string, ::_pbi::ConstantInitialized{}}
  , /*decltype(_impl_.error_)*/{&::_pbi::fixed_address_empty_string, ::_pbi::ConstantInitialized{}}
  , /*decltype(_impl_.computetimemicros_)*/int64_t{0}
  , /*decltype(_impl_.haserrored_)*/false
  , /*decltype(_impl_._cached_size_)*/{}} {}
struct MTMDefaultTypeInternal {
  PROTOBUF_CONSTEXPR MTMDefaultTypeInternal()
      : _instance(::_pbi::ConstantInitialized{}) {}
  ~MTMDefaultTypeInternal() {}
  union {
    MTM _instance;
  };
};
PROTOBUF_ATTRIBUTE_NO_DESTROY PROTOBUF_CONSTINIT PROTOBUF_ATTRIBUTE_INIT_PRIORITY1 MTMDefaultTypeInternal _MTM_default_instance_;
}  // namespace FlumaionQL
static ::_pb::Metadata file_level_metadata_mtms_2eproto[1];
static constexpr ::_pb::EnumDescriptor const** file_level_enum_descriptors_mtms_2eproto = nullptr;
static constexpr ::_pb::ServiceDescriptor const** file_level_service_descriptors_mtms_2eproto = nullptr;

const uint32_t TableStruct_mtms_2eproto::offsets[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  ~0u,  // no _has_bits_
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _internal_metadata_),
  ~0u,  // no _extensions_
  ~0u,  // no _oneof_case_
  ~0u,  // no _weak_field_map_
  ~0u,  // no _inlined_string_donated_
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.tradeid_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.curvename_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.fixlegdates_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.fixlegamount_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.fltlegdates_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.fltlegamount_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.discountvalues_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.legfractions_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.haserrored_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.error_),
  PROTOBUF_FIELD_OFFSET(::FlumaionQL::MTM, _impl_.computetimemicros_),
};
static const ::_pbi::MigrationSchema schemas[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) = {
  { 0, -1, -1, sizeof(::FlumaionQL::MTM)},
};

static const ::_pb::Message* const file_default_instances[] = {
  &::FlumaionQL::_MTM_default_instance_._instance,
};

const char descriptor_table_protodef_mtms_2eproto[] PROTOBUF_SECTION_VARIABLE(protodesc_cold) =
  "\n\nmtms.proto\022\nFlumaionQL\"\353\001\n\003MTM\022\017\n\007trad"
  "eid\030\001 \001(\t\022\021\n\tcurvename\030\002 \001(\t\022\023\n\013fixlegda"
  "tes\030\003 \003(\003\022\024\n\014fixlegamount\030\004 \003(\002\022\023\n\013fltle"
  "gdates\030\005 \003(\003\022\024\n\014fltlegamount\030\006 \003(\002\022\026\n\016di"
  "scountvalues\030\007 \003(\002\022\024\n\014legfractions\030\010 \003(\002"
  "\022\022\n\nhaserrored\030\t \001(\010\022\r\n\005error\030\n \001(\t\022\031\n\021c"
  "omputetimemicros\030\013 \001(\003b\006proto3"
  ;
static ::_pbi::once_flag descriptor_table_mtms_2eproto_once;
const ::_pbi::DescriptorTable descriptor_table_mtms_2eproto = {
    false, false, 270, descriptor_table_protodef_mtms_2eproto,
    "mtms.proto",
    &descriptor_table_mtms_2eproto_once, nullptr, 0, 1,
    schemas, file_default_instances, TableStruct_mtms_2eproto::offsets,
    file_level_metadata_mtms_2eproto, file_level_enum_descriptors_mtms_2eproto,
    file_level_service_descriptors_mtms_2eproto,
};
PROTOBUF_ATTRIBUTE_WEAK const ::_pbi::DescriptorTable* descriptor_table_mtms_2eproto_getter() {
  return &descriptor_table_mtms_2eproto;
}

// Force running AddDescriptors() at dynamic initialization time.
PROTOBUF_ATTRIBUTE_INIT_PRIORITY2 static ::_pbi::AddDescriptorsRunner dynamic_init_dummy_mtms_2eproto(&descriptor_table_mtms_2eproto);
namespace FlumaionQL {

// ===================================================================

class MTM::_Internal {
 public:
};

MTM::MTM(::PROTOBUF_NAMESPACE_ID::Arena* arena,
                         bool is_message_owned)
  : ::PROTOBUF_NAMESPACE_ID::Message(arena, is_message_owned) {
  SharedCtor(arena, is_message_owned);
  // @@protoc_insertion_point(arena_constructor:FlumaionQL.MTM)
}
MTM::MTM(const MTM& from)
  : ::PROTOBUF_NAMESPACE_ID::Message() {
  MTM* const _this = this; (void)_this;
  new (&_impl_) Impl_{
      decltype(_impl_.fixlegdates_){from._impl_.fixlegdates_}
    , /*decltype(_impl_._fixlegdates_cached_byte_size_)*/{0}
    , decltype(_impl_.fixlegamount_){from._impl_.fixlegamount_}
    , decltype(_impl_.fltlegdates_){from._impl_.fltlegdates_}
    , /*decltype(_impl_._fltlegdates_cached_byte_size_)*/{0}
    , decltype(_impl_.fltlegamount_){from._impl_.fltlegamount_}
    , decltype(_impl_.discountvalues_){from._impl_.discountvalues_}
    , decltype(_impl_.legfractions_){from._impl_.legfractions_}
    , decltype(_impl_.tradeid_){}
    , decltype(_impl_.curvename_){}
    , decltype(_impl_.error_){}
    , decltype(_impl_.computetimemicros_){}
    , decltype(_impl_.haserrored_){}
    , /*decltype(_impl_._cached_size_)*/{}};

  _internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
  _impl_.tradeid_.InitDefault();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
    _impl_.tradeid_.Set("", GetArenaForAllocation());
  #endif // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  if (!from._internal_tradeid().empty()) {
    _this->_impl_.tradeid_.Set(from._internal_tradeid(), 
      _this->GetArenaForAllocation());
  }
  _impl_.curvename_.InitDefault();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
    _impl_.curvename_.Set("", GetArenaForAllocation());
  #endif // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  if (!from._internal_curvename().empty()) {
    _this->_impl_.curvename_.Set(from._internal_curvename(), 
      _this->GetArenaForAllocation());
  }
  _impl_.error_.InitDefault();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
    _impl_.error_.Set("", GetArenaForAllocation());
  #endif // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  if (!from._internal_error().empty()) {
    _this->_impl_.error_.Set(from._internal_error(), 
      _this->GetArenaForAllocation());
  }
  ::memcpy(&_impl_.computetimemicros_, &from._impl_.computetimemicros_,
    static_cast<size_t>(reinterpret_cast<char*>(&_impl_.haserrored_) -
    reinterpret_cast<char*>(&_impl_.computetimemicros_)) + sizeof(_impl_.haserrored_));
  // @@protoc_insertion_point(copy_constructor:FlumaionQL.MTM)
}

inline void MTM::SharedCtor(
    ::_pb::Arena* arena, bool is_message_owned) {
  (void)arena;
  (void)is_message_owned;
  new (&_impl_) Impl_{
      decltype(_impl_.fixlegdates_){arena}
    , /*decltype(_impl_._fixlegdates_cached_byte_size_)*/{0}
    , decltype(_impl_.fixlegamount_){arena}
    , decltype(_impl_.fltlegdates_){arena}
    , /*decltype(_impl_._fltlegdates_cached_byte_size_)*/{0}
    , decltype(_impl_.fltlegamount_){arena}
    , decltype(_impl_.discountvalues_){arena}
    , decltype(_impl_.legfractions_){arena}
    , decltype(_impl_.tradeid_){}
    , decltype(_impl_.curvename_){}
    , decltype(_impl_.error_){}
    , decltype(_impl_.computetimemicros_){int64_t{0}}
    , decltype(_impl_.haserrored_){false}
    , /*decltype(_impl_._cached_size_)*/{}
  };
  _impl_.tradeid_.InitDefault();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
    _impl_.tradeid_.Set("", GetArenaForAllocation());
  #endif // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  _impl_.curvename_.InitDefault();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
    _impl_.curvename_.Set("", GetArenaForAllocation());
  #endif // PROTOBUF_FORCE_COPY_DEFAULT_STRING
  _impl_.error_.InitDefault();
  #ifdef PROTOBUF_FORCE_COPY_DEFAULT_STRING
    _impl_.error_.Set("", GetArenaForAllocation());
  #endif // PROTOBUF_FORCE_COPY_DEFAULT_STRING
}

MTM::~MTM() {
  // @@protoc_insertion_point(destructor:FlumaionQL.MTM)
  if (auto *arena = _internal_metadata_.DeleteReturnArena<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>()) {
  (void)arena;
    return;
  }
  SharedDtor();
}

inline void MTM::SharedDtor() {
  GOOGLE_DCHECK(GetArenaForAllocation() == nullptr);
  _impl_.fixlegdates_.~RepeatedField();
  _impl_.fixlegamount_.~RepeatedField();
  _impl_.fltlegdates_.~RepeatedField();
  _impl_.fltlegamount_.~RepeatedField();
  _impl_.discountvalues_.~RepeatedField();
  _impl_.legfractions_.~RepeatedField();
  _impl_.tradeid_.Destroy();
  _impl_.curvename_.Destroy();
  _impl_.error_.Destroy();
}

void MTM::SetCachedSize(int size) const {
  _impl_._cached_size_.Set(size);
}

void MTM::Clear() {
// @@protoc_insertion_point(message_clear_start:FlumaionQL.MTM)
  uint32_t cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  _impl_.fixlegdates_.Clear();
  _impl_.fixlegamount_.Clear();
  _impl_.fltlegdates_.Clear();
  _impl_.fltlegamount_.Clear();
  _impl_.discountvalues_.Clear();
  _impl_.legfractions_.Clear();
  _impl_.tradeid_.ClearToEmpty();
  _impl_.curvename_.ClearToEmpty();
  _impl_.error_.ClearToEmpty();
  ::memset(&_impl_.computetimemicros_, 0, static_cast<size_t>(
      reinterpret_cast<char*>(&_impl_.haserrored_) -
      reinterpret_cast<char*>(&_impl_.computetimemicros_)) + sizeof(_impl_.haserrored_));
  _internal_metadata_.Clear<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>();
}

const char* MTM::_InternalParse(const char* ptr, ::_pbi::ParseContext* ctx) {
#define CHK_(x) if (PROTOBUF_PREDICT_FALSE(!(x))) goto failure
  while (!ctx->Done(&ptr)) {
    uint32_t tag;
    ptr = ::_pbi::ReadTag(ptr, &tag);
    switch (tag >> 3) {
      // string tradeid = 1;
      case 1:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 10)) {
          auto str = _internal_mutable_tradeid();
          ptr = ::_pbi::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(ptr);
          CHK_(::_pbi::VerifyUTF8(str, "FlumaionQL.MTM.tradeid"));
        } else
          goto handle_unusual;
        continue;
      // string curvename = 2;
      case 2:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 18)) {
          auto str = _internal_mutable_curvename();
          ptr = ::_pbi::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(ptr);
          CHK_(::_pbi::VerifyUTF8(str, "FlumaionQL.MTM.curvename"));
        } else
          goto handle_unusual;
        continue;
      // repeated int64 fixlegdates = 3;
      case 3:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 26)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedInt64Parser(_internal_mutable_fixlegdates(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<uint8_t>(tag) == 24) {
          _internal_add_fixlegdates(::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr));
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      // repeated float fixlegamount = 4;
      case 4:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 34)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedFloatParser(_internal_mutable_fixlegamount(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<uint8_t>(tag) == 37) {
          _internal_add_fixlegamount(::PROTOBUF_NAMESPACE_ID::internal::UnalignedLoad<float>(ptr));
          ptr += sizeof(float);
        } else
          goto handle_unusual;
        continue;
      // repeated int64 fltlegdates = 5;
      case 5:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 42)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedInt64Parser(_internal_mutable_fltlegdates(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<uint8_t>(tag) == 40) {
          _internal_add_fltlegdates(::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr));
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      // repeated float fltlegamount = 6;
      case 6:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 50)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedFloatParser(_internal_mutable_fltlegamount(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<uint8_t>(tag) == 53) {
          _internal_add_fltlegamount(::PROTOBUF_NAMESPACE_ID::internal::UnalignedLoad<float>(ptr));
          ptr += sizeof(float);
        } else
          goto handle_unusual;
        continue;
      // repeated float discountvalues = 7;
      case 7:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 58)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedFloatParser(_internal_mutable_discountvalues(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<uint8_t>(tag) == 61) {
          _internal_add_discountvalues(::PROTOBUF_NAMESPACE_ID::internal::UnalignedLoad<float>(ptr));
          ptr += sizeof(float);
        } else
          goto handle_unusual;
        continue;
      // repeated float legfractions = 8;
      case 8:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 66)) {
          ptr = ::PROTOBUF_NAMESPACE_ID::internal::PackedFloatParser(_internal_mutable_legfractions(), ptr, ctx);
          CHK_(ptr);
        } else if (static_cast<uint8_t>(tag) == 69) {
          _internal_add_legfractions(::PROTOBUF_NAMESPACE_ID::internal::UnalignedLoad<float>(ptr));
          ptr += sizeof(float);
        } else
          goto handle_unusual;
        continue;
      // bool haserrored = 9;
      case 9:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 72)) {
          _impl_.haserrored_ = ::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr);
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      // string error = 10;
      case 10:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 82)) {
          auto str = _internal_mutable_error();
          ptr = ::_pbi::InlineGreedyStringParser(str, ptr, ctx);
          CHK_(ptr);
          CHK_(::_pbi::VerifyUTF8(str, "FlumaionQL.MTM.error"));
        } else
          goto handle_unusual;
        continue;
      // int64 computetimemicros = 11;
      case 11:
        if (PROTOBUF_PREDICT_TRUE(static_cast<uint8_t>(tag) == 88)) {
          _impl_.computetimemicros_ = ::PROTOBUF_NAMESPACE_ID::internal::ReadVarint64(&ptr);
          CHK_(ptr);
        } else
          goto handle_unusual;
        continue;
      default:
        goto handle_unusual;
    }  // switch
  handle_unusual:
    if ((tag == 0) || ((tag & 7) == 4)) {
      CHK_(ptr);
      ctx->SetLastTag(tag);
      goto message_done;
    }
    ptr = UnknownFieldParse(
        tag,
        _internal_metadata_.mutable_unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(),
        ptr, ctx);
    CHK_(ptr != nullptr);
  }  // while
message_done:
  return ptr;
failure:
  ptr = nullptr;
  goto message_done;
#undef CHK_
}

uint8_t* MTM::_InternalSerialize(
    uint8_t* target, ::PROTOBUF_NAMESPACE_ID::io::EpsCopyOutputStream* stream) const {
  // @@protoc_insertion_point(serialize_to_array_start:FlumaionQL.MTM)
  uint32_t cached_has_bits = 0;
  (void) cached_has_bits;

  // string tradeid = 1;
  if (!this->_internal_tradeid().empty()) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_tradeid().data(), static_cast<int>(this->_internal_tradeid().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "FlumaionQL.MTM.tradeid");
    target = stream->WriteStringMaybeAliased(
        1, this->_internal_tradeid(), target);
  }

  // string curvename = 2;
  if (!this->_internal_curvename().empty()) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_curvename().data(), static_cast<int>(this->_internal_curvename().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "FlumaionQL.MTM.curvename");
    target = stream->WriteStringMaybeAliased(
        2, this->_internal_curvename(), target);
  }

  // repeated int64 fixlegdates = 3;
  {
    int byte_size = _impl_._fixlegdates_cached_byte_size_.load(std::memory_order_relaxed);
    if (byte_size > 0) {
      target = stream->WriteInt64Packed(
          3, _internal_fixlegdates(), byte_size, target);
    }
  }

  // repeated float fixlegamount = 4;
  if (this->_internal_fixlegamount_size() > 0) {
    target = stream->WriteFixedPacked(4, _internal_fixlegamount(), target);
  }

  // repeated int64 fltlegdates = 5;
  {
    int byte_size = _impl_._fltlegdates_cached_byte_size_.load(std::memory_order_relaxed);
    if (byte_size > 0) {
      target = stream->WriteInt64Packed(
          5, _internal_fltlegdates(), byte_size, target);
    }
  }

  // repeated float fltlegamount = 6;
  if (this->_internal_fltlegamount_size() > 0) {
    target = stream->WriteFixedPacked(6, _internal_fltlegamount(), target);
  }

  // repeated float discountvalues = 7;
  if (this->_internal_discountvalues_size() > 0) {
    target = stream->WriteFixedPacked(7, _internal_discountvalues(), target);
  }

  // repeated float legfractions = 8;
  if (this->_internal_legfractions_size() > 0) {
    target = stream->WriteFixedPacked(8, _internal_legfractions(), target);
  }

  // bool haserrored = 9;
  if (this->_internal_haserrored() != 0) {
    target = stream->EnsureSpace(target);
    target = ::_pbi::WireFormatLite::WriteBoolToArray(9, this->_internal_haserrored(), target);
  }

  // string error = 10;
  if (!this->_internal_error().empty()) {
    ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::VerifyUtf8String(
      this->_internal_error().data(), static_cast<int>(this->_internal_error().length()),
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::SERIALIZE,
      "FlumaionQL.MTM.error");
    target = stream->WriteStringMaybeAliased(
        10, this->_internal_error(), target);
  }

  // int64 computetimemicros = 11;
  if (this->_internal_computetimemicros() != 0) {
    target = stream->EnsureSpace(target);
    target = ::_pbi::WireFormatLite::WriteInt64ToArray(11, this->_internal_computetimemicros(), target);
  }

  if (PROTOBUF_PREDICT_FALSE(_internal_metadata_.have_unknown_fields())) {
    target = ::_pbi::WireFormat::InternalSerializeUnknownFieldsToArray(
        _internal_metadata_.unknown_fields<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(::PROTOBUF_NAMESPACE_ID::UnknownFieldSet::default_instance), target, stream);
  }
  // @@protoc_insertion_point(serialize_to_array_end:FlumaionQL.MTM)
  return target;
}

size_t MTM::ByteSizeLong() const {
// @@protoc_insertion_point(message_byte_size_start:FlumaionQL.MTM)
  size_t total_size = 0;

  uint32_t cached_has_bits = 0;
  // Prevent compiler warnings about cached_has_bits being unused
  (void) cached_has_bits;

  // repeated int64 fixlegdates = 3;
  {
    size_t data_size = ::_pbi::WireFormatLite::
      Int64Size(this->_impl_.fixlegdates_);
    if (data_size > 0) {
      total_size += 1 +
        ::_pbi::WireFormatLite::Int32Size(static_cast<int32_t>(data_size));
    }
    int cached_size = ::_pbi::ToCachedSize(data_size);
    _impl_._fixlegdates_cached_byte_size_.store(cached_size,
                                    std::memory_order_relaxed);
    total_size += data_size;
  }

  // repeated float fixlegamount = 4;
  {
    unsigned int count = static_cast<unsigned int>(this->_internal_fixlegamount_size());
    size_t data_size = 4UL * count;
    if (data_size > 0) {
      total_size += 1 +
        ::_pbi::WireFormatLite::Int32Size(static_cast<int32_t>(data_size));
    }
    total_size += data_size;
  }

  // repeated int64 fltlegdates = 5;
  {
    size_t data_size = ::_pbi::WireFormatLite::
      Int64Size(this->_impl_.fltlegdates_);
    if (data_size > 0) {
      total_size += 1 +
        ::_pbi::WireFormatLite::Int32Size(static_cast<int32_t>(data_size));
    }
    int cached_size = ::_pbi::ToCachedSize(data_size);
    _impl_._fltlegdates_cached_byte_size_.store(cached_size,
                                    std::memory_order_relaxed);
    total_size += data_size;
  }

  // repeated float fltlegamount = 6;
  {
    unsigned int count = static_cast<unsigned int>(this->_internal_fltlegamount_size());
    size_t data_size = 4UL * count;
    if (data_size > 0) {
      total_size += 1 +
        ::_pbi::WireFormatLite::Int32Size(static_cast<int32_t>(data_size));
    }
    total_size += data_size;
  }

  // repeated float discountvalues = 7;
  {
    unsigned int count = static_cast<unsigned int>(this->_internal_discountvalues_size());
    size_t data_size = 4UL * count;
    if (data_size > 0) {
      total_size += 1 +
        ::_pbi::WireFormatLite::Int32Size(static_cast<int32_t>(data_size));
    }
    total_size += data_size;
  }

  // repeated float legfractions = 8;
  {
    unsigned int count = static_cast<unsigned int>(this->_internal_legfractions_size());
    size_t data_size = 4UL * count;
    if (data_size > 0) {
      total_size += 1 +
        ::_pbi::WireFormatLite::Int32Size(static_cast<int32_t>(data_size));
    }
    total_size += data_size;
  }

  // string tradeid = 1;
  if (!this->_internal_tradeid().empty()) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_tradeid());
  }

  // string curvename = 2;
  if (!this->_internal_curvename().empty()) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_curvename());
  }

  // string error = 10;
  if (!this->_internal_error().empty()) {
    total_size += 1 +
      ::PROTOBUF_NAMESPACE_ID::internal::WireFormatLite::StringSize(
        this->_internal_error());
  }

  // int64 computetimemicros = 11;
  if (this->_internal_computetimemicros() != 0) {
    total_size += ::_pbi::WireFormatLite::Int64SizePlusOne(this->_internal_computetimemicros());
  }

  // bool haserrored = 9;
  if (this->_internal_haserrored() != 0) {
    total_size += 1 + 1;
  }

  return MaybeComputeUnknownFieldsSize(total_size, &_impl_._cached_size_);
}

const ::PROTOBUF_NAMESPACE_ID::Message::ClassData MTM::_class_data_ = {
    ::PROTOBUF_NAMESPACE_ID::Message::CopyWithSourceCheck,
    MTM::MergeImpl
};
const ::PROTOBUF_NAMESPACE_ID::Message::ClassData*MTM::GetClassData() const { return &_class_data_; }


void MTM::MergeImpl(::PROTOBUF_NAMESPACE_ID::Message& to_msg, const ::PROTOBUF_NAMESPACE_ID::Message& from_msg) {
  auto* const _this = static_cast<MTM*>(&to_msg);
  auto& from = static_cast<const MTM&>(from_msg);
  // @@protoc_insertion_point(class_specific_merge_from_start:FlumaionQL.MTM)
  GOOGLE_DCHECK_NE(&from, _this);
  uint32_t cached_has_bits = 0;
  (void) cached_has_bits;

  _this->_impl_.fixlegdates_.MergeFrom(from._impl_.fixlegdates_);
  _this->_impl_.fixlegamount_.MergeFrom(from._impl_.fixlegamount_);
  _this->_impl_.fltlegdates_.MergeFrom(from._impl_.fltlegdates_);
  _this->_impl_.fltlegamount_.MergeFrom(from._impl_.fltlegamount_);
  _this->_impl_.discountvalues_.MergeFrom(from._impl_.discountvalues_);
  _this->_impl_.legfractions_.MergeFrom(from._impl_.legfractions_);
  if (!from._internal_tradeid().empty()) {
    _this->_internal_set_tradeid(from._internal_tradeid());
  }
  if (!from._internal_curvename().empty()) {
    _this->_internal_set_curvename(from._internal_curvename());
  }
  if (!from._internal_error().empty()) {
    _this->_internal_set_error(from._internal_error());
  }
  if (from._internal_computetimemicros() != 0) {
    _this->_internal_set_computetimemicros(from._internal_computetimemicros());
  }
  if (from._internal_haserrored() != 0) {
    _this->_internal_set_haserrored(from._internal_haserrored());
  }
  _this->_internal_metadata_.MergeFrom<::PROTOBUF_NAMESPACE_ID::UnknownFieldSet>(from._internal_metadata_);
}

void MTM::CopyFrom(const MTM& from) {
// @@protoc_insertion_point(class_specific_copy_from_start:FlumaionQL.MTM)
  if (&from == this) return;
  Clear();
  MergeFrom(from);
}

bool MTM::IsInitialized() const {
  return true;
}

void MTM::InternalSwap(MTM* other) {
  using std::swap;
  auto* lhs_arena = GetArenaForAllocation();
  auto* rhs_arena = other->GetArenaForAllocation();
  _internal_metadata_.InternalSwap(&other->_internal_metadata_);
  _impl_.fixlegdates_.InternalSwap(&other->_impl_.fixlegdates_);
  _impl_.fixlegamount_.InternalSwap(&other->_impl_.fixlegamount_);
  _impl_.fltlegdates_.InternalSwap(&other->_impl_.fltlegdates_);
  _impl_.fltlegamount_.InternalSwap(&other->_impl_.fltlegamount_);
  _impl_.discountvalues_.InternalSwap(&other->_impl_.discountvalues_);
  _impl_.legfractions_.InternalSwap(&other->_impl_.legfractions_);
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::InternalSwap(
      &_impl_.tradeid_, lhs_arena,
      &other->_impl_.tradeid_, rhs_arena
  );
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::InternalSwap(
      &_impl_.curvename_, lhs_arena,
      &other->_impl_.curvename_, rhs_arena
  );
  ::PROTOBUF_NAMESPACE_ID::internal::ArenaStringPtr::InternalSwap(
      &_impl_.error_, lhs_arena,
      &other->_impl_.error_, rhs_arena
  );
  ::PROTOBUF_NAMESPACE_ID::internal::memswap<
      PROTOBUF_FIELD_OFFSET(MTM, _impl_.haserrored_)
      + sizeof(MTM::_impl_.haserrored_)
      - PROTOBUF_FIELD_OFFSET(MTM, _impl_.computetimemicros_)>(
          reinterpret_cast<char*>(&_impl_.computetimemicros_),
          reinterpret_cast<char*>(&other->_impl_.computetimemicros_));
}

::PROTOBUF_NAMESPACE_ID::Metadata MTM::GetMetadata() const {
  return ::_pbi::AssignDescriptors(
      &descriptor_table_mtms_2eproto_getter, &descriptor_table_mtms_2eproto_once,
      file_level_metadata_mtms_2eproto[0]);
}

// @@protoc_insertion_point(namespace_scope)
}  // namespace FlumaionQL
PROTOBUF_NAMESPACE_OPEN
template<> PROTOBUF_NOINLINE ::FlumaionQL::MTM*
Arena::CreateMaybeMessage< ::FlumaionQL::MTM >(Arena* arena) {
  return Arena::CreateMessageInternal< ::FlumaionQL::MTM >(arena);
}
PROTOBUF_NAMESPACE_CLOSE

// @@protoc_insertion_point(global_scope)
#include <google/protobuf/port_undef.inc>
